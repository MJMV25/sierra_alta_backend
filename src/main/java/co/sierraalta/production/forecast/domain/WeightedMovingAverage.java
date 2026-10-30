package co.sierraalta.production.forecast.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Promedio móvil ponderado: P(t) = K0·D(t-1) + K1·D(t-2) + … El peso {@code weights[0]} (K0) se aplica
 * al dato más reciente, como en las fórmulas del Excel. Cada peso en (0, 1] y la suma igual a 1.
 */
public record WeightedMovingAverage(List<Double> weights) implements ForecastMethod {

	/** Margen para aceptar sumas como 0,3 + 0,2 + 0,2 + 0,2 + 0,1, que en binario no dan 1 exacto. */
	private static final double SUM_TOLERANCE = 1e-9;

	public WeightedMovingAverage {
		if (weights == null || weights.isEmpty()) {
			throw new InvalidForecastParameterException("weights", "Indique al menos un peso.");
		}
		double sum = 0;
		for (Double weight : weights) {
			if (weight == null || !(weight > 0 && weight <= 1)) {
				throw new InvalidForecastParameterException("weights",
						"Cada peso debe ser mayor que 0 y menor o igual que 1.");
			}
			sum += weight;
		}
		if (Math.abs(sum - 1) > SUM_TOLERANCE) {
			throw new InvalidForecastParameterException("weights", "Los pesos deben sumar exactamente 1.");
		}
		weights = List.copyOf(weights);
	}

	@Override
	public Forecast forecast(List<Double> actuals) {
		ForecastMethod.requireValidSeries(actuals);
		int periods = weights.size();
		MovingWindows.requireEnoughData(periods, actuals.size());
		List<Double> fitted = new ArrayList<>();
		for (int period = periods; period < actuals.size(); period++) {
			fitted.add(weightedBefore(actuals, period));
		}
		return new Forecast(periods, fitted, weightedBefore(actuals, actuals.size()));
	}

	private double weightedBefore(List<Double> actuals, int period) {
		double sum = 0;
		for (int lag = 1; lag <= weights.size(); lag++) {
			sum += weights.get(lag - 1) * actuals.get(period - lag);
		}
		return sum;
	}
}
