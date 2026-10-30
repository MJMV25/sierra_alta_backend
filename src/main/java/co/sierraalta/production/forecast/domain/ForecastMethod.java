package co.sierraalta.production.forecast.domain;

import java.util.List;

/**
 * Contrato común de los métodos de pronóstico (Strategy, ADR 04). Cada implementación reproduce una
 * columna de la hoja de cálculo del ANEXO A; los parámetros se validan al construirla.
 */
public interface ForecastMethod {

	/**
	 * @param actuals serie ordenada del periodo más antiguo al más reciente; al menos un dato, sin
	 *                valores negativos
	 */
	Forecast forecast(List<Double> actuals);

	/** Valida la serie de entrada: común a todos los métodos. */
	static void requireValidSeries(List<Double> actuals) {
		if (actuals == null || actuals.isEmpty()) {
			throw new InvalidForecastParameterException("series", "La serie no tiene datos.");
		}
		for (Double value : actuals) {
			if (value == null || !Double.isFinite(value) || value < 0) {
				throw new InvalidForecastParameterException("series",
						"La serie solo admite cantidades numéricas mayores o iguales a 0.");
			}
		}
	}

	/** α y β se aceptan solo estrictamente entre 0 y 1 (respuesta 10 de Industrial). */
	static double requireSmoothingFactor(String field, double value) {
		if (!(value > 0 && value < 1)) {
			throw new InvalidForecastParameterException(field, "Debe ser mayor que 0 y menor que 1.");
		}
		return value;
	}
}
