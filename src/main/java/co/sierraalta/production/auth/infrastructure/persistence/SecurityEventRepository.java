package co.sierraalta.production.auth.infrastructure.persistence;

import co.sierraalta.production.auth.domain.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

	Page<SecurityEvent> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);
}
