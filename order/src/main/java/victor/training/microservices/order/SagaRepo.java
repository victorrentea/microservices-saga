package victor.training.microservices.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaRepo extends JpaRepository<SagaState, Long> {
}
