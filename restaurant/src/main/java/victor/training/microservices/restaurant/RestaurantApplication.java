package victor.training.microservices.restaurant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Function;

@Slf4j
@SpringBootApplication
public class RestaurantApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestaurantApplication.class, args);
	}


	@Bean
	public Function<Message<String>, Message<String>> restaurant() {
		return request -> {
			log.info("Received restaurant request for " + request.getPayload());
			log.info("SAGA_ID="+request.getHeaders().get("SAGA_ID"));

			log.info("Cooking ...");
			sleep(1000);

			String response = "KO";

			log.info("Sending response: " + response);
			return MessageBuilder.createMessage(response, request.getHeaders());
		};
	}

	//<editor-fold desc="sleep">
	private static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	//</editor-fold>

}
