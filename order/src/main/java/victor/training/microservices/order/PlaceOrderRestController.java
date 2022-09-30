package victor.training.microservices.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import victor.training.microservices.message.DeliveryResponse;
import victor.training.microservices.message.PaymentResponse;
import victor.training.microservices.message.PaymentResponse.Status;
import victor.training.microservices.message.RestaurantResponse;


@RequiredArgsConstructor
@RestController
public class PlaceOrderRestController {
   private final PaymentClient paymentClient;
   private final RestaurantClient restaurantClient;
   private final DeliveryClient deliveryClient;

   @GetMapping("rest")
   public String restOrchestration() {
      String orderText = "Pizza Quatro Formaggi";
      PaymentResponse paymentResponse = paymentClient.processPayment(orderText);

      if (paymentResponse.getStatus() != Status.OK) {
         return "PAYMENT FAILED";
      }
      RestaurantResponse restaurantResponse = restaurantClient.requestCook("Please cook " + orderText);

      DeliveryResponse deliveryResponse = deliveryClient.findCourier(restaurantResponse.getDishId());

      if (deliveryResponse.getStatus() == DeliveryResponse.Status.COURIER_ASSIGNED) {
         return "COMPLETED";
      } else {
         return "Could not locate a courier";
      }

   }

}

@RequiredArgsConstructor
@Component
class PaymentClient {
   private final RestTemplate rest;
   public PaymentResponse processPayment(String orderText) {
      return rest.getForObject("payment/process/"+orderText, PaymentResponse.class);
   }
}
@RequiredArgsConstructor
@Component
class RestaurantClient {
   private final RestTemplate rest;
   public RestaurantResponse requestCook(String orderText) {
      return rest.getForObject("restaurant/process/"+orderText, RestaurantResponse.class);
   }
}
@RequiredArgsConstructor
@Component
class DeliveryClient {
   private final RestTemplate rest;
   public DeliveryResponse findCourier(String dishId) {
      return rest.getForObject("delivery/deliver/"+dishId, DeliveryResponse.class);
   }
}