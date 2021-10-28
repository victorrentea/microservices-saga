package victor.training.microservices.order.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@GlobalChannelInterceptor(patterns = "*-in-*")
@RequiredArgsConstructor
public class SagaContextMessageInterceptor implements ChannelInterceptor {
   private final SagaContext sagaContext;

   @Override
   public Message<?> preSend(Message<?> message, MessageChannel channel) {
      sagaContext.resumeSaga(message.getHeaders());
      String body = (message.getPayload() instanceof byte[] arr) ? new String(arr) : "?";
      log.info("Received message to {}: {}", channel, body);
      return message;
   }

   @Override
   public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
      sagaContext.flushSaga();
      ClearableThreadScope.clearAllThreadData();
   }

}