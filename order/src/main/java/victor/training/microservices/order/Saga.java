package victor.training.microservices.order;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;

import static javax.persistence.EnumType.STRING;

@Entity
@Data
public class Saga {
   @Id
   @GeneratedValue
   private Long id;

   private LocalDateTime createTime = LocalDateTime.now();
   @Enumerated(STRING)
   private Stage stage;

   enum Stage {
      AWAITING_PAYMENT,
      AWAITING_RESTAURANT,
      AWAITING_DELIVERY,
      COMPLETED,

      AWAITING_CANCEL_PAYMENT
   }
}
