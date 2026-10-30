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
 * Valor real de una serie en un mes, guardado con el primer día del mes. Un 0 es producción nula
 * (respuesta 14); un mes sin fila es un dato que no se ha cargado.
 */
@Entity
@Table(name = "observacion")
public class Observation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "serie_id", nullable = false, updatable = false)
	private Series series;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "carga_id", nullable = false, updatable = false)
	private DataLoad load;

	@Column(name = "periodo", nullable = false, updatable = false)
	private LocalDate period;

	@Column(name = "valor", nullable = false, precision = 18, scale = 4)
	private BigDecimal value;

	protected Observation() {
		// Requerido por JPA.
	}

	public Observation(Series series, DataLoad load, YearMonth period, BigDecimal value) {
		this.series = series;
		this.load = load;
		this.period = period.atDay(1);
		this.value = value;
	}

	public YearMonth getPeriod() {
		return YearMonth.from(period);
	}

	public BigDecimal getValue() {
		return value;
	}
}
