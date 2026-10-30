package co.sierraalta.production.history.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Serie de tiempo de un producto: una medida con una periodicidad y una unidad. */
@Entity
@Table(name = "serie")
public class Series {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "producto_id", nullable = false)
	private Product product;

	@Enumerated(EnumType.STRING)
	@Column(name = "medida", nullable = false, length = 30)
	private Measure measure;

	@Enumerated(EnumType.STRING)
	@Column(name = "periodicidad", nullable = false, length = 20)
	private Periodicity periodicity;

	@Column(name = "unidad", nullable = false, length = 20)
	private String unit;

	protected Series() {
		// Requerido por JPA.
	}

	public Series(Product product, Measure measure, Periodicity periodicity, String unit) {
		this.product = product;
		this.measure = measure;
		this.periodicity = periodicity;
		this.unit = unit;
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public Measure getMeasure() {
		return measure;
	}

	public Periodicity getPeriodicity() {
		return periodicity;
	}

	public String getUnit() {
		return unit;
	}
}
