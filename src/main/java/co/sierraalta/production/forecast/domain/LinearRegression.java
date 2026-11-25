package co.sierraalta.production.forecast.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Regresión lineal sobre el tiempo: P(t) = b·t + a, con t = 1..N y los mínimos cuadrados de Excel
 * ({@code PENDIENTE(D; t)} y {@code INTERSECCION.EJE(D; t)}, la demanda como Y y t como X). Corrección de
 * Industrial (documento "Error en el proyecto", 2026-11-06): el Excel tenía los argumentos del intercepto
 * invertidos y redondeaba P(t); aquí P(t) no se redondea.
 *
 * <p>La recta se ajusta con todos los datos y da un pronóstico para cada periodo desde t = 1, así que no
 * recorta la ventana común de evaluación; el pronóstico siguiente es b·(N+1) + a. No tiene parámetros.
 */
public record LinearRegression() implements ForecastMethod {

	@Override
	public Forecast forecast(List<Double> actuals) {
		ForecastMethod.requireValidSeries(actuals);
		int count = actuals.size();
		if (count < 2) {
			throw new InvalidForecastParameterException("method",
					"La regresión lineal necesita al menos 2 datos y la serie tiene " + count + ".");
		}
		double meanT = (count + 1) / 2.0;
		double meanD = actuals.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
		double covariance = 0;
		double variance = 0;
		for (int i = 0; i < count; i++) {
			double deviation = (i + 1) - meanT;
			covariance += deviation * (actuals.get(i) - meanD);
			variance += deviation * deviation;
		}
		double slope = covariance / variance;
		double intercept = meanD - slope * meanT;

		List<Double> fitted = new ArrayList<>();
		for (int t = 1; t <= count; t++) {
			fitted.add(slope * t + intercept);
		}
		return new Forecast(0, fitted, slope * (count + 1) + intercept);
	}
}
