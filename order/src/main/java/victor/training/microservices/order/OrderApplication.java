package victor.training.microservices.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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


@SpringBootApplication
@Slf4j
public class OrderApplication {

   public static void main(String[] args) {
      SpringApplication.run(OrderApplication.class, args);
   }
}
