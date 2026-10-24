package co.sierraalta.production.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de auditoría de acceso (tabla {@code evento}). Es inmutable una vez creado.
 *
 * <p>No guarda contraseñas, tokens ni identificadores de sesión. El usuario es opcional porque un
 * intento de inicio de sesión puede corresponder a un nombre inexistente.
 */
@Entity
@Table(name = "evento")
public class SecurityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "usuario_id", updatable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, updatable = false, length = 40)
	private SecurityEventType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "resultado", nullable = false, updatable = false, length = 20)
	private SecurityEventResult result;

	@Column(name = "fecha", nullable = false, updatable = false)
	private Instant occurredAt;

	protected SecurityEvent() {
		// Requerido por JPA.
	}

	public SecurityEvent(Long userId, SecurityEventType type, SecurityEventResult result) {
		this.userId = userId;
		this.type = type;
		this.result = result;
		this.occurredAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public SecurityEventType getType() {
		return type;
	}

	public SecurityEventResult getResult() {
		return result;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}
}
