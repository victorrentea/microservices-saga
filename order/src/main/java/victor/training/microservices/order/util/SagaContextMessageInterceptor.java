package victor.training.microservices.order.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import victor.training.microservices.order.SagaContext;

@Slf4j
@Component
@GlobalChannelInterceptor(patterns = "*-in-*")
@RequiredArgsConstructor
public class SagaContextMessageInterceptor implements ChannelInterceptor {
   private final SagaContext sagaContext;

   @Override
   public Message<?> preSend(Message<?> message, MessageChannel channel) {
      sagaContext.resumeSaga(message.getHeaders());
      MDC.put("sagaId","saga-" + sagaContext.getSagaId());
      return message;
   }

   @Override
   public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
      sagaContext.flushSaga();
      ClearableThreadScope.clearAllThreadData();
      MDC.clear();
   }

}