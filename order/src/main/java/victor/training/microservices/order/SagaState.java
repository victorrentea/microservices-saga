package victor.training.microservices.order;

import victor.training.microservices.order.ResponseMessage.PaymentFailedResponse;
import victor.training.microservices.order.ResponseMessage.PaymentSuccessfulResponse;

public sealed interface SagaState permits CompletedState, FailedState, AwaitingPaymentState{
   SagaState handleMessage(ResponseMessage message);

   // .send(this::requestPayment, this::undoPaymentMessage)
   // .receive(this::paymentSuccessful)
   // .
   // .then(request(restaurant(), undoRestaurant(),
}


sealed interface ResponseMessage {

   record PaymentSuccessfulResponse(Long paymentId) implements ResponseMessage {
   }
   record PaymentFailedResponse(String errorMessage) implements ResponseMessage {
   }
}

final class CompletedState implements SagaState {
   @Override
   public SagaState handleMessage(ResponseMessage message) {
      throw new RuntimeException("End of line");
   }
}
final class FailedState implements SagaState {
   @Override
   public SagaState handleMessage(ResponseMessage message) {
      return null;
   }
}
final class AwaitingPaymentState implements SagaState {

   private final Saga saga;

   AwaitingPaymentState(Saga saga) {
      this.saga = saga;
   }

   @Override
   public SagaState handleMessage(ResponseMessage message) {
      return switch (message) {
         case PaymentSuccessfulResponse r -> new CompletedState();
         case PaymentFailedResponse r -> {
            saga.setErrorMessage(r.errorMessage());
            yield new FailedState();
         }
//         default ->
      };
   }
}