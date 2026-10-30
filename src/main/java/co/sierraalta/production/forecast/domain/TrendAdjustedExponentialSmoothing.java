package co.sierraalta.production.forecast.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Suavizamiento exponencial con ajuste a la tendencia (Holt), con los valores iniciales fijos del Excel:
 * A(1) = D(1), T(1) = 0. Para t ≥ 2: P(t) = A(t-1) + T(t-1); A(t) = α·D(t) + (1-α)·P(t);
 * T(t) = β·(A(t) - A(t-1)) + (1-β)·T(t-1). Se pronostica solo el periodo siguiente: A(N) + T(N).
 */
public record TrendAdjustedExponentialSmoothing(double alpha, double beta) implements ForecastMethod {

	public TrendAdjustedExponentialSmoothing {
		ForecastMethod.requireSmoothingFactor("alpha", alpha);
		ForecastMethod.requireSmoothingFactor("beta", beta);
	}

	@Override
	public Forecast forecast(List<Double> actuals) {
		ForecastMethod.requireValidSeries(actuals);
		List<Double> fitted = new ArrayList<>();
		double level = actuals.get(0);
		double trend = 0;
		for (int period = 1; period < actuals.size(); period++) {
			double forecast = level + trend;
			fitted.add(forecast);
			double newLevel = alpha * actuals.get(period) + (1 - alpha) * forecast;
			trend = beta * (newLevel - level) + (1 - beta) * trend;
			level = newLevel;
		}
		return new Forecast(1, fitted, level + trend);
	}
}
