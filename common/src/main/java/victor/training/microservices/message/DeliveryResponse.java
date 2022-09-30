package victor.training.microservices.message;

import lombok.Data;

@Data
public class DeliveryResponse {
   public enum Status {
      COURIER_ASSIGNED,
      COURIER_NOT_FOUND
   }
   private Status status;
   private String courierPhone;
   private String courierName;
   private String courierId;

}
