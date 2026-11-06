package co.sierraalta.production.forecast.domain;

import java.util.ArrayList;
import java.util.List;

/** Promedio móvil simple: P(t) = (D(t-1) + … + D(t-n)) / n. Primer pronóstico en el periodo n+1. */
public record SimpleMovingAverage(int periods) implements ForecastMethod {

	public SimpleMovingAverage {
		if (periods < 1) {
			throw new InvalidForecastParameterException("periods", "Debe ser un entero mayor o igual a 1.");
		}
	}

	@Override
	public Forecast forecast(List<Double> actuals) {
		ForecastMethod.requireValidSeries(actuals);
		MovingWindows.requireEnoughData(periods, actuals.size());
		List<Double> fitted = new ArrayList<>();
		for (int period = periods; period < actuals.size(); period++) {
			fitted.add(averageBefore(actuals, period));
		}
		return new Forecast(periods, fitted, averageBefore(actuals, actuals.size()));
	}

	private double averageBefore(List<Double> actuals, int period) {
		double sum = 0;
		for (int lag = 1; lag <= periods; lag++) {
			sum += actuals.get(period - lag);
		}
		return sum / periods;
	}
}
