package victor.training.microservices.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaEntityRepo extends JpaRepository<SagaEntity, Long> {
}
