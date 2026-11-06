package co.sierraalta.production.forecast.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Pronóstico de un método para un periodo (tabla {@code resultado}); {@code actual} solo en EVALUACION. */
@Embeddable
public class ResultPoint {

	@Column(name = "periodo", nullable = false)
	private LocalDate period;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private ResultType type;

	@Column(name = "valor_pronosticado", nullable = false, precision = 24, scale = 10)
	private BigDecimal forecast;

	@Column(name = "valor_real", precision = 18, scale = 4)
	private BigDecimal actual;

	protected ResultPoint() {
		// Requerido por JPA.
	}

	private ResultPoint(YearMonth period, ResultType type, BigDecimal forecast, BigDecimal actual) {
		this.period = period.atDay(1);
		this.type = type;
		this.forecast = forecast;
		this.actual = actual;
	}

	public static ResultPoint evaluation(YearMonth period, BigDecimal forecast, BigDecimal actual) {
		return new ResultPoint(period, ResultType.EVALUACION, forecast, actual);
	}

	public static ResultPoint estimation(YearMonth period, BigDecimal forecast) {
		return new ResultPoint(period, ResultType.ESTIMACION, forecast, null);
	}

	public YearMonth getPeriod() {
		return YearMonth.from(period);
	}

	public ResultType getType() {
		return type;
	}

	public BigDecimal getForecast() {
		return forecast;
	}

	public BigDecimal getActual() {
		return actual;
	}
}
