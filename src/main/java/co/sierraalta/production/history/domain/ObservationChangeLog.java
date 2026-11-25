package co.sierraalta.production.history.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Historial de la hoja (ADR 13): un mes que cambió en un guardado. Quién y cuándo están en la carga.
 * {@code previous} nulo es un mes agregado; {@code next} nulo, un mes borrado. No se modifica.
 */
@Entity
@Table(name = "cambio_observacion")
public class ObservationChangeLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "carga_id", nullable = false, updatable = false)
	private DataLoad load;

	@Column(name = "periodo", nullable = false, updatable = false)
	private LocalDate period;

	@Column(name = "valor_anterior", precision = 18, scale = 4, updatable = false)
	private BigDecimal previous;

	@Column(name = "valor_nuevo", precision = 18, scale = 4, updatable = false)
	private BigDecimal next;

	protected ObservationChangeLog() {
		// Requerido por JPA.
	}

	public ObservationChangeLog(DataLoad load, PlannedChange change) {
		this.load = load;
		this.period = change.period().atDay(1);
		this.previous = change.previous();
		this.next = change.next();
	}

	public DataLoad getLoad() {
		return load;
	}

	public YearMonth getPeriod() {
		return YearMonth.from(period);
	}

	public BigDecimal getPrevious() {
		return previous;
	}

	public BigDecimal getNext() {
		return next;
	}
}
