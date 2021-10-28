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
import victor.training.microservices.order.Saga;
import victor.training.microservices.order.SagaRepo;

import static org.springframework.messaging.support.MessageBuilder.withPayload;

@Slf4j
@Component
@RequiredArgsConstructor
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SagaContext implements DisposableBean {
   private final SagaRepo sagaRepo;
   private final StreamBridge streamBridge;
   private Saga saga;

   public Saga startSaga() {
      if (saga != null) {
         throw new IllegalStateException("Another saga is already in progress");
      }
      saga = sagaRepo.save(new Saga());
      saga.setMessageSender(this::sendMessage);
      MDC.put("sagaId","saga-" + saga.getId());
      log.info("Started saga id {}", saga.getId());
      return saga;
   }

   void resumeSaga(MessageHeaders incomingHeaders) {
      Long sagaId = (Long) incomingHeaders.get("SAGA_ID");
      saga = sagaRepo.findById(sagaId).get();
      saga.setMessageSender(this::sendMessage);
      MDC.put("sagaId","saga-" + sagaId);
      log.debug("<< Resumed saga id {}: {}", sagaId, saga);
   }

   public Saga currentSaga() {
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

   void flushSaga() {
      if (saga != null) {
         log.debug(">> Writing saga to DB: " + saga);
         sagaRepo.save(saga);
      }
      MDC.clear();
   }

   @Override
   public void destroy() throws Exception {
      flushSaga();
   }
}
