package victor.training.microservices.order;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import victor.training.microservices.message.PaymentResponse;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static javax.persistence.EnumType.STRING;

@Slf4j
@Entity
@Data
public class Saga {
   @Id
   @GeneratedValue
   private Long id;
   @Enumerated(STRING)
   private Stage stage;
   private String orderText;
   private String paymentConfirmationNumber;
   private String restaurantDishId;
   private LocalDateTime createTime = LocalDateTime.now();
   private String errorMessage;

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
      AWAITING_PAYMENT,// (sendMessage(Saga), receiveMessage(Saga, M):Stage, undoAction(Saga)),//(PaymentResponse.class, Saga::paymentResponse),
      AWAITING_RESTAURANT,// (sendRestaurantReq(Saga), receiveRestResponse),
      AWAITING_DELIVERY,
      COMPLETED,
      FAILED
   }

   public void paymentResponse(PaymentResponse response, BiConsumer<String, Object> messageSender) {
      if (stage != Stage.AWAITING_PAYMENT) {
         throw new IllegalStateException();
      }
      if (response.getStatus() == PaymentResponse.Status.KO) {
         log.error("SAGA failed at step PAYMENT. All fine: nothing to undo");
         stage = Stage.FAILED;
      } else {
         paymentConfirmationNumber = response.getPaymentConfirmationNumber();
         stage = Stage.AWAITING_RESTAURANT;
         messageSender.accept("restaurantRequest-out-0", "Please cook " + orderText);
      }
   }
   class SagaStage<RQ, RS, U> {
      private final Stage stage;
      private final Supplier<RQ> send;
      private final Consumer<RS> receive;
      private Supplier<U> undo;

      SagaStage(Stage stage, Supplier<RQ> send, Consumer<RS> receive) {
         this.stage = stage;
         this.send = send;
         this.receive = receive;
      }
   }
}


