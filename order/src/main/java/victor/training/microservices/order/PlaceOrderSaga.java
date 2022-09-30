package victor.training.microservices.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.microservices.message.DeliveryResponse;
import victor.training.microservices.message.PaymentResponse;
import victor.training.microservices.message.RestaurantResponse;
import victor.training.microservices.order.SagaState.Status;
import victor.training.microservices.order.context.SagaContext;

import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.time.LocalDateTime.now;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PlaceOrderSaga {
   private final SagaContext sagaContext;

   @GetMapping
   public String startMessageSaga() {
      SagaState sagaState = sagaContext.startSaga();
      String orderText = "Pizza Order " + now().format(DateTimeFormatter.ISO_LOCAL_TIME);
      sagaState.setOrderText(orderText);
      sagaContext.sendMessage("paymentRequest-out-0", orderText);
      sagaState.setStatus(Status.AWAITING_PAYMENT);
      return "Message sent!";
   }

   private <T> Consumer<T> wrap(BiConsumer<T, BiConsumer<String, Object>> sagaMethod) {
      return response -> sagaMethod.accept(response, sagaContext::sendMessage);
   }

   @Bean
   public Consumer<PaymentResponse> paymentResponse() {
      // Trick: sagaContext.currentSaga() returns the saga fetched from DB by the 'SAGA_ID' header in the incoming message
      return response -> {
         if (sagaContext.currentSaga().getStatus() != Status.AWAITING_PAYMENT) {
            throw new IllegalStateException("I should be waiting for PAYMENT response");
         }
         if (response.getStatus() == PaymentResponse.Status.OK) {
            sagaContext.currentSaga().setPaymentConfirmationNumber(response.getPaymentConfirmationNumber());
            sagaContext.sendMessage("restaurantRequest-out-0", "Please cook " + sagaContext.currentSaga().getOrderText());
            sagaContext.currentSaga().setStatus(Status.AWAITING_RESTAURANT);
         } else {
            log.error("SAGA failed at step PAYMENT. All fine: nothing to undo");
            sagaContext.currentSaga().setStatus(Status.FAILED);
         }
      };
      // Trick: the currentSaga() is auto-flushed at the end automatically
   }

   @Bean
   public Consumer<RestaurantResponse> restaurantResponse() {
      return response -> {
         if (sagaContext.currentSaga().getStatus() != Status.AWAITING_RESTAURANT) {
            throw new IllegalStateException("I should be waiting for RESTAURANT response");
         }
         if (response.getStatus() != RestaurantResponse.Status.ORDER_ACCEPTED) {
            String dishId = response.getDishId();
            sagaContext.currentSaga().setRestaurantDishId(dishId);
            sagaContext.sendMessage("deliveryRequest-out-0", "Deliver dishId " + dishId);
            sagaContext.currentSaga().setStatus(Status.AWAITING_DELIVERY);
         } else {
            log.error("SAGA failed at step RESTAURANT");
            sagaContext.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + sagaContext.currentSaga().getPaymentConfirmationNumber());
            sagaContext.currentSaga().setStatus(Status.FAILED);
         }
      };
   }

   @Bean
   public Consumer<DeliveryResponse> deliveryResponse() {
      return response -> {
         if (sagaContext.currentSaga().getStatus() != Status.AWAITING_DELIVERY) {
            throw new IllegalStateException("I should be waiting for DELIVERY response");
         }
         if (response.getStatus() == DeliveryResponse.Status.COURIER_ASSIGNED) {
            log.info("SAGA completed OK");
            sagaContext.currentSaga().setCourierPhone(response.getCourierPhone());
            sagaContext.currentSaga().setStatus(Status.COMPLETED);
         } else {
            log.error("SAGA failed at step DELIVERY");
            sagaContext.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + sagaContext.currentSaga().getPaymentConfirmationNumber());
            sagaContext.sendMessage("restaurantUndoRequest-out-0", "Cancel cooking dish ID:" + sagaContext.currentSaga().getRestaurantDishId());
            sagaContext.currentSaga().setStatus(Status.FAILED);
         }
      };
   }

}
