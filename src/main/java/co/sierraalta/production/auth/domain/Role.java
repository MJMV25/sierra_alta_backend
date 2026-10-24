package co.sierraalta.production.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Rol del catálogo. Las filas se crean por migración; la aplicación no las modifica. */
@Entity
@Table(name = "rol")
public class Role {

	@Id
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "codigo", nullable = false, unique = true, updatable = false)
	private RoleCode code;

	@Column(name = "nombre", nullable = false)
	private String name;

	protected Role() {
		// Requerido por JPA.
	}

	public Long getId() {
		return id;
	}

	public RoleCode getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
