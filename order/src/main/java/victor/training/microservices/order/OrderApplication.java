package victor.training.microservices.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@RestController
public class OrderApplication {
   private final StreamBridge streamBridge;
	private static final AtomicInteger counter = new AtomicInteger();

   @GetMapping
   public String startSaga() {
      String sagaId = UUID.randomUUID().toString();
      MessageBuilder.withPayload("Hello world!")
          .setHeader("SAGA_ID", sagaId);
		String orderId = "Order " + counter.incrementAndGet();
		String correlationId = RandomStringUtils.randomAlphabetic(6); // use in prod UUID.randomUUID().toString();
		streamBridge.send("paymentRequest", MessageBuilder.createMessage(orderId, new MessageHeaders(Map.of("CORRELATION_ID", correlationId))));
      log.info("Started SAGA_ID: " + sagaId);
      return "Message sent!";
   }

   @Bean
   public Consumer<Message<String>> paymentResponse() {
      return responseMessage -> {
			String response = responseMessage.getPayload();
			log.info("Got response from payment: " + response);

			if (response.equals("OK")) {
				// continue flow
				Message<String> requestMessage = MessageBuilder.createMessage("Cook " + response, responseMessage.getHeaders());
				streamBridge.send("restaurantRequest", requestMessage);
			} else {
				log.error("SAGA failed at step PAYMENT");
			}
      };
   }

   @Bean
   public Consumer<Message<String>> restaurantResponse() {
      return responseMessage -> {
			String response = responseMessage.getPayload();
			log.info("Got response from restaurant: " + response);

			if (response.equals("OK")) {
				// continue flow
				log.info("SAGA completed");
			} else {
				log.error("SAGA failed at step RESTAURANT");
				log.info("Sending cancel payment request");
				streamBridge.send("paymentUndoRequest",MessageBuilder.createMessage("Reimburse payment ", responseMessage.getHeaders()));
			}
      };
   }

	@Bean
	public Consumer<Message<String>> paymentUndoResponse() {
		return responseMessage -> {
			String response = responseMessage.getPayload();
			log.info("Got response from cancel payment: " + response);

			if (response.equals("OK")) {
				log.info("SAGA cancelled successfully");
			} else {
				log.error("Cancel payment operation failed");
				log.error("Sending an email");
			}
		};
	}


   public static void main(String[] args) {
      SpringApplication.run(OrderApplication.class, args);
   }

}
