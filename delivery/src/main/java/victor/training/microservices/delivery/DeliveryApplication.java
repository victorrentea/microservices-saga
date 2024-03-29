package victor.training.microservices.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import victor.training.microservices.common.Sleep;
import victor.training.microservices.message.DeliveryResponse;
import victor.training.microservices.message.DeliveryResponse.Status;

import java.util.function.Function;

@Slf4j
@SpringBootApplication
public class DeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveryApplication.class, args);
	}

	@Bean
	public Function<Message<String>, Message<DeliveryResponse>> delivery() {
		return request -> {
			log.info("Received delivery request: " + request.getPayload());
			log.info("SAGA_ID="+request.getHeaders().get("SAGA_ID"));

			log.info("Finding delivery man ...");
			Sleep.sleepQuiet(1000);

			DeliveryResponse response = new DeliveryResponse()
				.setStatus(Status.COURIER_ASSIGNED)
				.setCourierPhone("::phone::")
//				.setStatus(Status.COURIER_NOT_FOUND)
				;

			log.info("Sending reply: " + response);
			return MessageBuilder.createMessage(response, request.getHeaders());
		};
	}


}
