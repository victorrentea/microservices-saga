package victor.training.microservices.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.microservices.message.DeliveryResponse;
import victor.training.microservices.message.DeliveryResponse.Status;
import victor.training.microservices.message.PaymentResponse;
import victor.training.microservices.message.RestaurantResponse;
import victor.training.microservices.order.Saga.Stage;
import victor.training.microservices.order.context.SagaContext;

import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.time.LocalDateTime.now;


@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@RestController
public class OrderApplication {
	private final SagaContext context;

   @GetMapping
   public String startSaga() {
		Saga saga = context.startSaga();
		String orderText = "Pizza Order " + now().format(DateTimeFormatter.ISO_LOCAL_TIME);
		saga.setOrderText(orderText);
		context.sendMessage("paymentRequest-out-0", orderText);
		saga.setStage(Stage.AWAITING_PAYMENT);
      return "Message sent!";
   }

	private <T> Consumer<T> wrap(BiConsumer<T, BiConsumer<String, Object>> sagaMethod) {
		return response -> sagaMethod.accept(response, context::sendMessage);
	}

   @Bean
	public Consumer<PaymentResponse> paymentResponse() {
		return response -> context.currentSaga().paymentResponse(response);
		// or:
//		return response -> {
//			if (context.currentSaga().getStage() != Stage.AWAITING_PAYMENT) {
//				throw new IllegalStateException();
//			}
//			if (response.getStatus() == PaymentResponse.Status.KO) {
//				log.error("SAGA failed at step PAYMENT. All fine: nothing to undo");
//				context.currentSaga().setStage(Stage.FAILED);
//			} else {
//				context.currentSaga().setPaymentConfirmationNumber(response.getPaymentConfirmationNumber());
//				context.sendMessage("restaurantRequest-out-0", "Please cook " + context.currentSaga().getOrderText());
//				context.currentSaga().setStage(Stage.AWAITING_RESTAURANT);
//			}
//		};
	}

	@Bean
   public Consumer<RestaurantResponse> restaurantResponse() {
      return response -> {
			if (context.currentSaga().getStage() != Stage.AWAITING_RESTAURANT) {
				throw new IllegalStateException();
			}
			if (response.getStatus() == RestaurantResponse.Status.DISH_UNAVAILABLE) {
				log.error("SAGA failed at step RESTAURANT");
				context.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + context.currentSaga().getPaymentConfirmationNumber());
				context.currentSaga().setStage(Stage.FAILED);
			} else {
				String dishId = response.getDishId();
				context.currentSaga().setRestaurantDishId(dishId);
				context.sendMessage("deliveryRequest-out-0", "Deliver dishId " + dishId);
				context.currentSaga().setStage(Stage.AWAITING_DELIVERY);
			}
		};
   }

	@Bean
   public Consumer<DeliveryResponse> deliveryResponse() {
      return response -> {
			if (context.currentSaga().getStage() != Stage.AWAITING_DELIVERY) {
				throw new IllegalStateException();
			}
			if (response.getStatus() == Status.BOOKED) {
				log.info("SAGA completed OK");
				context.currentSaga().setCourierPhone(response.getCourierPhone());
				context.currentSaga().setStage(Stage.COMPLETED);
			} else {
				log.error("SAGA failed at step DELIVERY");
				context.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + context.currentSaga().getPaymentConfirmationNumber());
				context.sendMessage("restaurantUndoRequest-out-0", "Cancel cooking dish ID:" + context.currentSaga().getRestaurantDishId());
				context.currentSaga().setStage(Stage.FAILED);
			}
      };
   }



   public static void main(String[] args) {
      SpringApplication.run(OrderApplication.class, args);
   }

}
