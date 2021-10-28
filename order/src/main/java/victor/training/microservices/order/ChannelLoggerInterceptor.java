package victor.training.microservices.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChannelLoggerInterceptor implements ChannelInterceptor {
   @Override
   public Message<?> preSend(Message<?> message, MessageChannel channel) {
      log.info("Sending message " + message.getPayload() + " to " + channel + " with headers: " + message.getHeaders());
      return message;
   }
}
