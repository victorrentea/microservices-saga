package victor.training.microservices.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import static org.springframework.messaging.support.MessageBuilder.withPayload;

@Slf4j
@Component
@RequiredArgsConstructor
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SagaContext {
   private final SagaEntityRepo sagaRepo;
   private final StreamBridge streamBridge;
   private SagaEntity saga;

   public SagaEntity startSaga() {
      if (saga != null) {
         throw new IllegalStateException("Another saga is already in progress");
      }
      saga = sagaRepo.save(new SagaEntity());
      log.info("Started saga id {}", saga.getId());
      return saga;
   }

   public void resumeSaga(MessageHeaders incomingHeaders) {
      Long sagaId = (Long) incomingHeaders.get("SAGA_ID");
      saga = sagaRepo.findById(sagaId).get();
      log.warn("Resumed saga id {}: {}", sagaId, saga);
   }

   public SagaEntity currentSaga() {
      return saga;
   }

   public <T> void sendMessage(String outChannel, T payload) {
      Message<T> requestMessage = withPayload(payload)
          .setHeader("SAGA_ID", saga.getId())
          .build();
      log.info("Sending message to {}: {}", outChannel, payload);
      streamBridge.send(outChannel, requestMessage);
   }

   public Long getSagaId() {
      return saga.getId();
   }

   public void flushSaga() {
      if (saga != null) {
         log.warn("Writing saga to DB: " + saga);
         sagaRepo.save(saga);
      }
   }
}
