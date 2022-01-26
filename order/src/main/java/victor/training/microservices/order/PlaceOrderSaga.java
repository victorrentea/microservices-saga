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
   private final SagaContext context;



   @GetMapping
   public String startMessageSaga() {
      SagaState sagaState = context.startSaga();
      String orderText = "Pizza Order " + now().format(DateTimeFormatter.ISO_LOCAL_TIME);
      sagaState.setOrderText(orderText);
      context.sendMessage("paymentRequest-out-0", orderText);
      sagaState.setStatus(Status.AWAITING_PAYMENT);
      return "Message sent!";
   }

   private <T> Consumer<T> wrap(BiConsumer<T, BiConsumer<String, Object>> sagaMethod) {
      return response -> sagaMethod.accept(response, context::sendMessage);
   }

   @Bean
   public Consumer<PaymentResponse> paymentResponse() {
//      if (true)
//         return response -> context.currentSaga().handleAnyResponse(response); // State pattern
//      if (true)
//         return response -> context.currentSaga().paymentResponse(response); // OOP
//      else
      return response -> { // transaction script
         if (context.currentSaga().getStatus() != Status.AWAITING_PAYMENT) {
            throw new IllegalStateException();
         }
         if (response.getStatus() == PaymentResponse.Status.KO) {
            log.error("SAGA failed at step PAYMENT. All fine: nothing to undo");
            context.currentSaga().setStatus(Status.FAILED);
         } else {
            context.currentSaga().setPaymentConfirmationNumber(response.getPaymentConfirmationNumber());
            context.sendMessage("restaurantRequest-out-0", "Please cook " + context.currentSaga().getOrderText());
            context.currentSaga().setStatus(Status.AWAITING_RESTAURANT);
         }
      };
   }

   @Bean
   public Consumer<RestaurantResponse> restaurantResponse() {
      if (true) {
         return response -> context.currentSaga().handleAnyResponse(response);
      } else { // OR ...
         return response -> {
            if (context.currentSaga().getStatus() != Status.AWAITING_RESTAURANT) {
               throw new IllegalStateException();
            }
            if (response.getStatus() == RestaurantResponse.Status.DISH_UNAVAILABLE) {
               log.error("SAGA failed at step RESTAURANT");
               context.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + context.currentSaga().getPaymentConfirmationNumber());
               context.currentSaga().setStatus(Status.FAILED);
            } else {
               String dishId = response.getDishId();
               context.currentSaga().setRestaurantDishId(dishId);
               context.sendMessage("deliveryRequest-out-0", "Deliver dishId " + dishId);
               context.currentSaga().setStatus(Status.AWAITING_DELIVERY);
            }
         };
      }
   }

   @Bean
   public Consumer<DeliveryResponse> deliveryResponse() {
      if (true) {
         return response -> context.currentSaga().handleAnyResponse(response);
      } else { // OR
         return response -> {
            if (context.currentSaga().getStatus() != Status.AWAITING_DELIVERY) {
               throw new IllegalStateException();
            }
            if (response.getStatus() == DeliveryResponse.Status.BOOKED) {
               log.info("SAGA completed OK");
               context.currentSaga().setCourierPhone(response.getCourierPhone());
//               websocketReposiory.findClient().send("ORDER ACCEPTED")
               context.currentSaga().setStatus(Status.COMPLETED);
            } else {
               log.error("SAGA failed at step DELIVERY");
               context.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + context.currentSaga().getPaymentConfirmationNumber());
               context.sendMessage("restaurantUndoRequest-out-0", "Cancel cooking dish ID:" + context.currentSaga().getRestaurantDishId());
               context.currentSaga().setStatus(Status.FAILED);
            }
         };
      }
   }

}
