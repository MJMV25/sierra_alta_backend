package co.sierraalta.production.forecast.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Dato real usado por una ejecución (tabla {@code entrada}): no cambia aunque se corrija el histórico. */
@Embeddable
public class InputValue {

	@Column(name = "periodo", nullable = false)
	private LocalDate period;

	@Column(name = "valor", nullable = false, precision = 18, scale = 4)
	private BigDecimal value;

	protected InputValue() {
		// Requerido por JPA.
	}

	public InputValue(YearMonth period, BigDecimal value) {
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
