package co.sierraalta.production.forecast.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Suavizamiento exponencial simple: F(1) = D(1) (valor inicial fijo, como en el Excel) y
 * F(t+1) = α·D(t) + (1-α)·F(t). F(1) es la semilla, no un pronóstico: el primero es el del periodo 2.
 */
public record ExponentialSmoothing(double alpha) implements ForecastMethod {

	public ExponentialSmoothing {
		ForecastMethod.requireSmoothingFactor("alpha", alpha);
	}

	@Override
	public Forecast forecast(List<Double> actuals) {
		ForecastMethod.requireValidSeries(actuals);
		List<Double> fitted = new ArrayList<>();
		double current = actuals.get(0);
		for (int period = 1; period <= actuals.size(); period++) {
			current = alpha * actuals.get(period - 1) + (1 - alpha) * current;
			if (period < actuals.size()) {
				fitted.add(current);
			}
		}
		return new Forecast(1, fitted, current);
	}
}
