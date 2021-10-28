package victor.training.microservices.order.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@GlobalChannelInterceptor
@RequiredArgsConstructor
public class ChannelLoggerInterceptor implements ChannelInterceptor {

   @Override
   public Message<?> preSend(Message<?> message, MessageChannel channel) {
      String body = (message.getPayload() instanceof byte[] arr) ? new String(arr) : "?";
//      if (!(channel instanceof DirectWithAttributesChannel)) {
      if (channel.toString().startsWith("bean"))
         log.info("Received message to {}: {}", channel, body);
//      }
      return message;
   }

   private String getChannelReadableName(MessageChannel channel) {
      String channelName;
      if (channel instanceof DirectWithAttributesChannel direct) {
         channelName = "<manual send>";
      } else {
         channelName = channel.toString();
      }
      return channelName;
   }
}
