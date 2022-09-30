package victor.training.microservices.order;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import victor.training.microservices.message.DeliveryResponse;
import victor.training.microservices.message.PaymentResponse;
import victor.training.microservices.message.RestaurantResponse;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static javax.persistence.EnumType.STRING;

@RequiredArgsConstructor
@Slf4j
@Entity
@Data
public class SagaState {
   @Id
   @GeneratedValue
   private Long id;
   @Enumerated(STRING)
   private Status status;
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
   enum Status {
      AWAITING_PAYMENT(PaymentStage.class),// (sendMessage(Saga), receiveMessage(Saga, M):Stage, undoAction(Saga)),//(PaymentResponse.class, Saga::paymentResponse),
      AWAITING_RESTAURANT(RestaurantStage.class),// (sendRestaurantReq(Saga), receiveRestResponse),
      AWAITING_DELIVERY(DeliveryStage.class),

      COMPLETED(null),
      FAILED(null);

      private final Class<? extends Stage> messageSender;
   }

   sealed abstract class Stage permits PaymentStage, RestaurantStage, DeliveryStage {
      protected final Logger log = LoggerFactory.getLogger(getClass());

      public abstract void request();

      public abstract Status receive(Object message);

      public void undo() {
         log.warn("Undo not applicable");
      }
   }
      final class PaymentStage extends Stage {
         public void request() {
            messageSender.sendMessage("paymentRequest-out-0", orderText);
         }

         public Status receive(Object message) {
            if (message instanceof PaymentResponse response) {
               if (response.getStatus() == PaymentResponse.Status.KO) {
                  throw new IllegalArgumentException();
               }
               paymentConfirmationNumber = response.getPaymentConfirmationNumber();
               return Status.AWAITING_RESTAURANT;
            } else {
               throw new IllegalArgumentException();
            }
         }

         public void undo() {
            messageSender.sendMessage("paymentUndoRequest-out-0", "Revert payment confirmation number:" + paymentConfirmationNumber);
         }
      }

      final class RestaurantStage extends Stage {
         public void request() {
            messageSender.sendMessage("restaurantRequest-out-0", "Please cook " + orderText);
         }

         public Status receive(Object message) {
            if (message instanceof RestaurantResponse response) {
               if (response.getStatus() == RestaurantResponse.Status.DISH_UNAVAILABLE) {
                  throw new IllegalArgumentException();
               }
               restaurantDishId = response.getDishId();
               return Status.AWAITING_DELIVERY;
            } else {
               throw new IllegalArgumentException();
            }
         }

         public void undo() {
            messageSender.sendMessage("restaurantUndoRequest-out-0", "Cancel cooking dish ID:" + restaurantDishId);
         }
      }

      final class DeliveryStage extends Stage {
         public void request() {
            messageSender.sendMessage("deliveryRequest-out-0", "Deliver dishId " + restaurantDishId);
         }

         public Status receive(Object message) {
            if (message instanceof DeliveryResponse response) {
               if (response.getStatus() == DeliveryResponse.Status.COURIER_NOT_FOUND) {
                  throw new IllegalArgumentException("Courier NOT available");
                   // or
//                  return Status.FAILED;
               }
               courierPhone = response.getCourierPhone();
               return Status.COMPLETED;
            } else {
               throw new IllegalArgumentException();
            }
         }
      }

   public void handleAnyResponseUsingStatePattern(Object responsePayload) {
      log.info("Received response message: " + responsePayload);
      Status newStatus = tryExecuteRequest(responsePayload);

      if (newStatus == Status.FAILED) {
         undoPreviousSteps();
      }

      log.info("Moving from {} -> {}", status, newStatus);
      if (newStatus.ordinal() < status.ordinal()) {
         throw new IllegalArgumentException("You cannot return to a previous status");
      }

      status = newStatus;

      switch (status) {
         case FAILED -> log.warn("SAGA FAILED");
         case COMPLETED -> log.info("SAGA Completed");
         default -> currentStage(status).request();
      }
   }

   @SneakyThrows
   private void undoPreviousSteps() {
      for (int i = status.ordinal() - 1; i >= 0; i--) {
         currentStage(Status.values()[i]).undo();
      }
      log.warn("Sent all UNDO requests. ");
   }

   private Status tryExecuteRequest(Object responsePayload) {
      try {
         return currentStage(status).receive(responsePayload);
      } catch (Exception e) {
         log.error("SAGA ERROR (full stack on TRACE): " + e);
         log.trace("Exception details: ", e);
         return Status.FAILED;
      }
   }

   @SneakyThrows
   private Stage currentStage(Status status) {
      // Note: inner instance classes
      return status.messageSender.getDeclaredConstructor(SagaState.class).newInstance(this);
   }

   public void paymentResponse(PaymentResponse response) {
      if (status != Status.AWAITING_PAYMENT) {
         throw new IllegalStateException();
      }
      if (response.getStatus() == PaymentResponse.Status.KO) {
         log.error("SAGA failed at step PAYMENT. All fine: nothing to undo");
         status = Status.FAILED;
      } else {
         paymentConfirmationNumber = response.getPaymentConfirmationNumber();
         status = Status.AWAITING_RESTAURANT;
         messageSender.sendMessage("restaurantRequest-out-0", "Please cook " + orderText);
      }
   }

   class SagaStage<RQ, RS, U> {
      private final Status stage;
      private final Supplier<RQ> send;
      private final Consumer<RS> receive;
      private Supplier<U> undo;

      SagaStage(Status stage, Supplier<RQ> send, Consumer<RS> receive) {
         this.stage = stage;
         this.send = send;
         this.receive = receive;
      }
   }
}


