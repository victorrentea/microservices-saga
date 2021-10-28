package victor.training.microservices.common;

public class Sleep {
   public static void sleep(int millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }
}
