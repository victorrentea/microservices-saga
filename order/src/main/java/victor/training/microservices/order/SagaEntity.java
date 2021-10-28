package victor.training.microservices.order;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;

import static javax.persistence.EnumType.STRING;

@Entity
@Data
public class SagaEntity {
   @Id
   @GeneratedValue
   private Long id;

   private LocalDateTime createTime = LocalDateTime.now();
   @Enumerated(STRING)
   private Stage stage = Stage.AWAITING_PAYMENT;
   private String errorMessage;
   private String orderText;
   private String paymentConfirmationNumber;
   private String restaurantDishId;

   public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
   }

   public String getErrorMessage() {
      return errorMessage;
   }

   public void setOrderText(String orderText) {
      this.orderText = orderText;
   }

   public String getOrderText() {
      return orderText;
   }

   public void setPaymentConfirmationNumber(String paymentConfirmationNumber) {
      this.paymentConfirmationNumber = paymentConfirmationNumber;
   }

   public String getPaymentConfirmationNumber() {
      return paymentConfirmationNumber;
   }

   public String getRestaurantDishId() {
      return restaurantDishId;
   }

   public void setRestaurantDishId(String restaurantDishId) {
      this.restaurantDishId = restaurantDishId;
   }

   enum Stage {
      AWAITING_PAYMENT,
      AWAITING_RESTAURANT,
      AWAITING_DELIVERY,
      COMPLETED,
   }
}
