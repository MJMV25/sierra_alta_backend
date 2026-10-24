package co.sierraalta.production.auth.application;

import co.sierraalta.production.auth.domain.SecurityEvent;
import co.sierraalta.production.auth.domain.SecurityEventResult;
import co.sierraalta.production.auth.domain.SecurityEventType;
import co.sierraalta.production.auth.infrastructure.persistence.SecurityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra eventos de seguridad en una transacción propia, para que un intento fallido quede
 * auditado aunque la operación que lo originó se revierta.
 */
@Service
public class SecurityEventRecorder {

	private final SecurityEventRepository events;

	public SecurityEventRecorder(SecurityEventRepository events) {
		this.events = events;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(Long userId, SecurityEventType type, SecurityEventResult result) {
		events.save(new SecurityEvent(userId, type, result));
	}
}
