package victor.training.microservices.message;

import lombok.Data;

@Data
public class RestaurantResponse {
   public enum Status {
      ORDER_ACCEPTED,
      DISH_UNAVAILABLE
   }
   private Status status;
   private String dishId;

}
