package victor.training.microservices.message;

import lombok.Data;

@Data
public class PaymentResponse {
   public enum Status {
      OK,
      KO
   }
   private Status status;
   private String paymentConfirmationNumber;

}
