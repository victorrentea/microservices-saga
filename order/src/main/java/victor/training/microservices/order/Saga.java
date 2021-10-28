package victor.training.microservices.order;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import victor.training.microservices.message.PaymentResponse;
import victor.training.microservices.message.PaymentResponse.Status;
import victor.training.microservices.order.Saga.State.AwaitingPaymentState;

import javax.persistence.*;
import java.time.LocalDateTime;
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

   @Transient
   private MessageSender messageSender;
   private String courierPhone;


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

   public void setCourierPhone(String courierPhone) {
      this.courierPhone = courierPhone;
   }

   public String getCourierPhone() {
      return courierPhone;
   }

   @RequiredArgsConstructor
   enum Stage {
      AWAITING_PAYMENT(AwaitingPaymentState.class),// (sendMessage(Saga), receiveMessage(Saga, M):Stage, undoAction(Saga)),//(PaymentResponse.class, Saga::paymentResponse),
      AWAITING_RESTAURANT(null),// (sendRestaurantReq(Saga), receiveRestResponse),
      AWAITING_DELIVERY(null),

      COMPLETED(null),
      FAILED(null);

      private final Class<? extends State> messageSender;
   }

   sealed abstract class State {
      protected final Logger log = LoggerFactory.getLogger(getClass());
      public abstract void request();
      public abstract Stage receive(Object message);
      public void undo() {
         log.debug("Undo N/A");
      }

      final class AwaitingPaymentState extends State {
         public void request() {
            messageSender.sendMessage("paymentRequest-out-0", orderText);
         }
         public Stage receive(Object message) {
            if (message instanceof PaymentResponse response) {
               if (response.getStatus() == Status.KO) {
                  throw new IllegalArgumentException();
               }
               paymentConfirmationNumber = response.getPaymentConfirmationNumber();
               return Stage.AWAITING_RESTAURANT;
            } else {
               throw new IllegalArgumentException();
            }
         }
         public void undo() {
            messageSender.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + paymentConfirmationNumber);
         }
      }

//      final class AwaitingRestaurant extends State {
//         public void request() {
//            messageSender.sendMessage("restaurantRequest-out-0", "Please cook " + orderText);
//         }
//         public Stage receive(Object message) {
//            if (message instanceof String response) {
//               if (response.getStatus() == Status.KO) {
//                  throw new IllegalArgumentException();
//               }
//               paymentConfirmationNumber = response.getPaymentConfirmationNumber();
//               messageSender.sendMessage("restaurantRequest-out-0", "Please cook " + orderText);
//               return Stage.AWAITING_RESTAURANT;
//            } else {
//               throw new IllegalArgumentException();
//            }
//         }
//      }
   }

   public void paymentResponse(PaymentResponse response) {
      if (stage != Stage.AWAITING_PAYMENT) {
         throw new IllegalStateException();
      }
      if (response.getStatus() == Status.KO) {
         log.error("SAGA failed at step PAYMENT. All fine: nothing to undo");
         stage = Stage.FAILED;
      } else {
         paymentConfirmationNumber = response.getPaymentConfirmationNumber();
         stage = Stage.AWAITING_RESTAURANT;
         messageSender.sendMessage("restaurantRequest-out-0", "Please cook " + orderText);
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


