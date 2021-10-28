package victor.training.microservices.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.microservices.order.SagaEntity.Stage;
import victor.training.microservices.order.context.ClearableThreadScope;
import victor.training.microservices.order.context.SagaContext;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import static java.time.LocalDateTime.now;


@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@RestController
public class OrderApplication {
	private final SagaContext context;

	@Bean
	public static CustomScopeConfigurer defineThreadScope() {
		CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.addScope("thread", new ClearableThreadScope());
		return configurer;
	}

   @GetMapping
   public String startSaga() {
		SagaEntity saga = context.startSaga();
		String orderText = "Pizza Order " + now().format(DateTimeFormatter.ISO_LOCAL_TIME);
		saga.setOrderText(orderText);
		context.sendMessage("paymentRequest-out-0", orderText);
		saga.setStage(Stage.AWAITING_PAYMENT);
      return "Message sent!";
   }

   @Bean
   public Consumer<String> paymentResponse() {
      return response -> {
			if (context.currentSaga().getStage() != Stage.AWAITING_PAYMENT) {
				throw new IllegalStateException();
			}
			if (response.equalsIgnoreCase("KO")) {
				log.error("SAGA failed at step PAYMENT. All fine: nothing to undo");
				context.currentSaga().setStage(Stage.FAILED);
			} else {
				context.currentSaga().setPaymentConfirmationNumber(response);
				context.sendMessage("restaurantRequest-out-0", "Please cook " + context.currentSaga().getOrderText());
				context.currentSaga().setStage(Stage.AWAITING_RESTAURANT);
			}
		};
   }

	@Bean
   public Consumer<String> restaurantResponse() {
      return response -> {
			if (context.currentSaga().getStage() != Stage.AWAITING_RESTAURANT) {
				throw new IllegalStateException();
			}
			if (response.equals("KO")) {
				log.error("SAGA failed at step RESTAURANT");
				context.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + context.currentSaga().getPaymentConfirmationNumber());
				context.currentSaga().setStage(Stage.FAILED);
			} else {
				String dishId = response;
				context.currentSaga().setRestaurantDishId(dishId);
				context.sendMessage("deliveryRequest-out-0", "Deliver dishId " + dishId);
				context.currentSaga().setStage(Stage.AWAITING_DELIVERY);
			}
		};
   }

	@Bean
   public Consumer<String> deliveryResponse() {
      return response -> {
			if (context.currentSaga().getStage() != Stage.AWAITING_DELIVERY) {
				throw new IllegalStateException();
			}
			if (response.equals("OK")) {
				log.info("SAGA completed OK");
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
