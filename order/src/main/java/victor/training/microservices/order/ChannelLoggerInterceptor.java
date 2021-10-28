package victor.training.microservices.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@GlobalChannelInterceptor
public class ChannelLoggerInterceptor implements ChannelInterceptor {
   private static final ThreadLocal<String> ORIGINAL_THREAD_NAME = new ThreadLocal<>();
   public static final ThreadLocal<String> SAGA_ID = new ThreadLocal<>();

   @Override
   public Message<?> preSend(Message<?> message, MessageChannel channel) {
      MessageHeaders headers = message.getHeaders();
      String sagaId = (String) headers.get("SAGA_ID");
      SAGA_ID.set(sagaId);
      ORIGINAL_THREAD_NAME.set(Thread.currentThread().getName());
      Thread.currentThread().setName("saga-" + SAGA_ID.get());
      String body = (message.getPayload() instanceof byte[] arr) ? new String(arr) : "?";
      log.info("Sending message to {}: {}", channel, body);
      return message;
   }

   @Override
   public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
      if (ORIGINAL_THREAD_NAME.get() != null) {
//         log.info("Restoring original thread name : " + ORIGINAL_THREAD_NAME.get());
         Thread.currentThread().setName(ORIGINAL_THREAD_NAME.get());
         ORIGINAL_THREAD_NAME.remove();
      }
      SAGA_ID.remove();
   }

   private String getChannelReadableName(MessageChannel channel) {
      String channelName;
//      if (channel instanceof DirectWithAttributesChannel direct) {
//         channelName = direct.getFullChannelName();
//      } else {
         channelName = channel.toString();
//      }
      return channelName;
   }
}
