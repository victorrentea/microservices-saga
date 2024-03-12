package victor.training.microservices.order.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import victor.training.microservices.order.SagaState;
import victor.training.microservices.order.SagaRepo;

import static org.springframework.messaging.support.MessageBuilder.withPayload;

@Slf4j
@Component
@RequiredArgsConstructor
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SagaContext implements DisposableBean {
   private final SagaRepo sagaRepo;
   private final StreamBridge streamBridge;
   private SagaState sagaState;

   public SagaState startSaga() {
      if (sagaState != null) {
         throw new IllegalStateException("Another saga is already in progress");
      }
      sagaState = sagaRepo.save(new SagaState());
      sagaState.setMessageSender(this::sendMessage);
      MDC.put("sagaId", "saga-" + sagaState.getId());
      log.info("Started saga id {}", sagaState.getId());
      return sagaState;
   }

   void resumeSaga(MessageHeaders incomingHeaders) {
      Long sagaId = (Long) incomingHeaders.get("SAGA_ID");
      sagaState = sagaRepo.findById(sagaId).get();
      sagaState.setMessageSender(this::sendMessage);
      MDC.put("sagaId","saga-" + sagaId);
      log.debug("<< Resumed saga id {}: {}", sagaId, sagaState);
   }

   public SagaState saga() {
      return sagaState;
   }

   public <T> void sendMessage(String outChannel, T payload) {
      Message<T> requestMessage = withPayload(payload)
          .setHeader("SAGA_ID", sagaState.getId())
          .build();
      log.info("Sending message to {}: {}", outChannel, payload);
      streamBridge.send(outChannel, requestMessage);
   }

   public Long getSagaId() {
      return sagaState.getId();
   }

   void saveSagaInDB() {
      if (sagaState != null) {
         log.debug(">> Writing saga to DB: " + sagaState);
         sagaRepo.save(sagaState);
      }
      MDC.clear();
   }

   @Override
   public void destroy() throws Exception {
      saveSagaInDB();
   }
}
