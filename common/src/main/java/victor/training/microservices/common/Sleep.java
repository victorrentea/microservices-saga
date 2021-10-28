package victor.training.microservices.common;

public class Sleep {
   public static void sleepQuiet(int millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }
}
