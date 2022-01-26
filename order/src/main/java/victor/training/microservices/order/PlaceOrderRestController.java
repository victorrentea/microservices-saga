//package victor.training.microservices.order;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//import victor.training.microservices.message.DeliveryResponse;
//import victor.training.microservices.message.PaymentResponse;
//import victor.training.microservices.message.PaymentResponse.Status;
//import victor.training.microservices.message.RestaurantResponse;
//
//
//@RequiredArgsConstructor
//@RestController
//public class PlaceOrderRestController {
//   private final PaymentClient paymentClient;
//   private final RestaurantClient restaurantClient;
//   private final DeliveryClient deliveryClient;
//
//   @GetMapping("rest")
//   public String restOrchestration() {
//      String orderText = "Pizza Quatro Formaggi";
//
//      PaymentResponse paymentResponse = paymentClient.processPayment(orderText); // BLOCKING your thread for 5-15 seconds
//      try {
//         if (paymentResponse.getStatus() != Status.OK) {
//            // TODO DELETE order
//            return "PAYMENT FAILED";
//         }
//
//         RestaurantResponse restaurantResponse = restaurantClient.requestCook("Please cook " + orderText);
//
//         if (restaurantResponse.getStatus() != RestaurantResponse.Status.ORDER_ACCEPTED) {
//            return "NO MORE FOOD";
//         }
//
//         DeliveryResponse deliveryResponse = deliveryClient.findCourier(restaurantResponse.getDishId());
//
//         if (deliveryResponse.getStatus() == DeliveryResponse.Status.BOOKED) {
//            try {
////               restaurantClient.startCooking(orderText);
//            } catch (Exception e) {
////               deliveryClient.cancelGoHome(deliveryResponse.getCourierPhone());
//               throw e;
//            }
//
//            return "COMPLETED";
//         } else {
//            throw new RuntimeException("Could not locate a courier");
//         }
//      } catch (RuntimeException e) {
//         //         paymentClient.reimburse(paymentResponse.getPaymentConfirmationNumber()); // compensation / undo transaction
//         throw e;
//      }
//
//   }
//
//}
//
//@RequiredArgsConstructor
//@Component
//class PaymentClient {
//   private final RestTemplate rest;
//
////   @Retryable
//   public PaymentResponse processPayment(String orderText) {
//      return rest.getForObject("payment/process/" + orderText, PaymentResponse.class);
//   }
//}
//
//@RequiredArgsConstructor
//@Component
//class RestaurantClient {
//   private final RestTemplate rest;
//
//   public RestaurantResponse requestCook(String orderText) {
//      return rest.getForObject("restaurant/process/" + orderText, RestaurantResponse.class);
//   }
//}
//
//@RequiredArgsConstructor
//@Component
//class DeliveryClient {
//   private final RestTemplate rest;
//
//   public DeliveryResponse findCourier(String dishId) {
//      return rest.getForObject("delivery/deliver/" + dishId, DeliveryResponse.class);
//   }
//}