package co.sierraalta.production.history.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Referencia de producto (por ejemplo, Jersey Pro). El catálogo se crea por migración. Las de ejemplo
 * (Spokane y ACME, casos del Excel) se muestran aparte y son de solo lectura (ADR 13).
 */
@Entity
@Table(name = "producto")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "codigo", nullable = false, unique = true, length = 40)
	private String code;

	@Column(name = "nombre", nullable = false, length = 120)
	private String name;

	@Column(name = "activo", nullable = false)
	private boolean active;

	@Column(name = "ejemplo", nullable = false)
	private boolean sample;

	protected Product() {
		// Requerido por JPA.
	}

	public Product(String code, String name) {
		this.code = code;
		this.name = name;
		this.active = true;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isSample() {
		return sample;
	}
}
