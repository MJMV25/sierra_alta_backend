package co.sierraalta.production.auth.application;

import java.time.Instant;

import co.sierraalta.production.auth.domain.SecurityEvent;
import co.sierraalta.production.auth.domain.SecurityEventResult;
import co.sierraalta.production.auth.domain.SecurityEventType;

public record SecurityEventSummary(Long id, Long userId, SecurityEventType type, SecurityEventResult result,
		Instant occurredAt) {

	static SecurityEventSummary from(SecurityEvent event) {
		return new SecurityEventSummary(event.getId(), event.getUserId(), event.getType(), event.getResult(),
				event.getOccurredAt());
	}
}
