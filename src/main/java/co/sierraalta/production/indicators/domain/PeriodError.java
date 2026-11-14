package co.sierraalta.production.indicators.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Error de un periodo, como las columnas del Excel: E(t) = D(t) − P(t) y |E(t)| / D(t) en porcentaje. Es lo
 * que promedian el MAD y el MAPE; aquí se entrega periodo a periodo para la tabla de resultados.
 *
 * @param error           real menos pronóstico (negativo si se pronosticó de más)
 * @param absolutePercent |E(t)| / D(t) × 100; nulo si el real es 0 (no entra al MAPE)
 */
public record PeriodError(BigDecimal error, BigDecimal absolutePercent) {

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private static final int SCALE = 10;

	/** Nulo si falta el real (el pronóstico del periodo siguiente). */
	public static PeriodError of(BigDecimal actual, BigDecimal forecast) {
		if (actual == null || forecast == null) {
			return null;
		}
		BigDecimal error = actual.subtract(forecast);
		BigDecimal percent = actual.signum() == 0 ? null
				: error.abs().multiply(HUNDRED).divide(actual, MathContext.DECIMAL64).setScale(SCALE, RoundingMode.HALF_UP);
		return new PeriodError(error, percent);
	}
}
