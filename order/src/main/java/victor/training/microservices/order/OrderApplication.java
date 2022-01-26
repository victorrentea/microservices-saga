package victor.training.microservices.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication
@Slf4j
public class OrderApplication {

   @Bean
   public RestTemplate restTemplate() {
      return new RestTemplate();
   }

   public static void main(String[] args) {
      SpringApplication.run(OrderApplication.class, args);
   }
}
