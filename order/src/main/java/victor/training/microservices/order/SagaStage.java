package victor.training.microservices.order;

import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

enum SagaStageCode {
   COMPLETED
}

interface SagaStage {
   SagaStageCode code();

   Message<?> requestMessage();
   SagaStage receiveMessage(Message<?> response, Consumer<Message<?>> messageSender);

   boolean hasUndo();
   Message<?> undoMessage();
}

class Seq{
   List<SagaStage> stages = new ArrayList<>();
}

class SagaStagePlay {
   public static void main(String[] args) {

//      new SagaStage()
   }
}
