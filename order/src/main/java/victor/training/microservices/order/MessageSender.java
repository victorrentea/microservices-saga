package victor.training.microservices.order;

public interface MessageSender {
   void sendMessage(String channel, Object payload);
}
