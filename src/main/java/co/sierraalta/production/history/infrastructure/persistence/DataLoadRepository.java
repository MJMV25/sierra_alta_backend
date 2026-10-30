package co.sierraalta.production.history.infrastructure.persistence;

import co.sierraalta.production.history.domain.DataLoad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataLoadRepository extends JpaRepository<DataLoad, Long> {
}
