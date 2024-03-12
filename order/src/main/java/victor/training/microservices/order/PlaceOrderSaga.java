package victor.training.microservices.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.microservices.message.DeliveryResponse;
import victor.training.microservices.message.PaymentResponse;
import victor.training.microservices.message.RestaurantResponse;
import victor.training.microservices.order.context.SagaContext;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import static java.time.LocalDateTime.now;
import static victor.training.microservices.message.DeliveryResponse.Status.COURIER_ASSIGNED;
import static victor.training.microservices.message.PaymentResponse.Status.*;
import static victor.training.microservices.message.RestaurantResponse.Status.ORDER_ACCEPTED;
import static victor.training.microservices.order.SagaState.Status.*;

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
    context.sendMessage("process-payment-command", orderText);
    sagaState.setStatus(AWAITING_PAYMENT);
    return "Message sent!";
  }

  @Bean // the new cloud-stream "functional-style" message handlers
  public Consumer<PaymentResponse> paymentResponse() {
    // Trick: sagaContext.saga() returns the saga fetched from DB by the 'SAGA_ID' header in the incoming message
    return paymentResponse -> {
      if (context.saga().status() != AWAITING_PAYMENT) {
        throw new IllegalStateException("I should be waiting for PAYMENT response");
      }
      if (paymentResponse.getStatus() == OK) {
        context.saga().setPaymentConfirmationNumber(paymentResponse.getPaymentConfirmationNumber());
        context.sendMessage("cook-food-command", "Please cook " + context.saga().getOrderText());
        context.saga().setStatus(AWAITING_RESTAURANT);
      } else {
        log.error("SAGA failed at step PAYMENT: nothing to undo");
        context.saga().setStatus(FAILED);
      }
    };
    // Magic warning⚠️: the saga() is auto-flushed to DB at the end of this method automatically
  }

  @Bean
  public Consumer<RestaurantResponse> restaurantResponse() {
    return restaurantResponse -> {
      context.saga().checkHasStatus(AWAITING_RESTAURANT);
      
      if (restaurantResponse.getStatus() == ORDER_ACCEPTED) {
        String dishId = restaurantResponse.getDishId();
        context.saga().setRestaurantDishId(dishId);
        context.sendMessage("find-courier-command", "Deliver dishId " + dishId);
        context.saga().setStatus(AWAITING_DELIVERY);
      } else {
        log.error("SAGA failed at step RESTAURANT");
        context.sendMessage("revert-payment-command", "Revert payment number:" + context.saga().getPaymentConfirmationNumber());
        context.saga().setStatus(FAILED);
      }
    };
  }

  @Bean
  public Consumer<DeliveryResponse> deliveryResponse() {
    return deliveryResponse -> {
      context.saga().checkHasStatus(AWAITING_DELIVERY);

      if (deliveryResponse.getStatus() == COURIER_ASSIGNED) {
        log.info("SAGA completed OK");
        context.saga().setCourierPhone(deliveryResponse.getCourierPhone());
        context.saga().setStatus(COMPLETED);
      } else {
        log.error("SAGA failed at step DELIVERY");
        context.sendMessage("revert-payment-command", "Revert payment confirmation number:" + context.saga().getPaymentConfirmationNumber());
        context.sendMessage("restaurantUndoRequest-out-0", "Cancel cooking dish ID:" + context.saga().getRestaurantDishId());
        context.saga().setStatus(FAILED);
      }
    };
  }

}
