package victor.training.microservices.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import victor.training.microservices.common.Sleep;
import victor.training.microservices.message.PaymentResponse;
import victor.training.microservices.message.PaymentResponse.Status;

import java.util.function.Consumer;
import java.util.function.Function;
@Slf4j
@SpringBootApplication
public class PaymentApplication {
	public static void main(String[] args) {
		SpringApplication.run(PaymentApplication.class, args);
	}

	@Bean
	public Function<Message<String>, Message<PaymentResponse>> payment() {
		return request -> {
			log.info("Received payment request for " + request.getPayload());
			log.info("SAGA_ID="+request.getHeaders().get("SAGA_ID"));

			log.info("Processing payment ...");
			Sleep.sleepQuiet(1000);

//			String response = "KO";
//			String response = "#" + System.currentTimeMillis();
			PaymentResponse response = new PaymentResponse()
				.setStatus(Status.OK)
				.setPaymentConfirmationNumber("#" + System.currentTimeMillis());

			log.info("Sending response: " + response);
			return MessageBuilder.createMessage(response, request.getHeaders());
		};
	}

	@Bean
	public Consumer<Message<String>> paymentUndo() {
		return request -> {
			log.info("Received request to undo payment " + request.getPayload());
			log.info("SAGA_ID="+request.getHeaders().get("SAGA_ID"));

			log.info("Reverting payment ...");
			Sleep.sleepQuiet(1000);

			String response = "OK";

			log.info("PAYMENT REVERTED: " + response);

			// throw new RuntimeException("Revert payment failed!");

//			return MessageBuilder.createMessage(response, request.getHeaders());
		};
	}


}
