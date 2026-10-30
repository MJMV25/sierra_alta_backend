package co.sierraalta.production.forecast.domain;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import co.sierraalta.production.indicators.domain.ForecastPair;

/**
 * Resultado de un método sobre una serie de N periodos (índices 0..N-1).
 *
 * @param firstPeriod índice del primer periodo con pronóstico
 * @param fitted      pronósticos de los periodos {@code firstPeriod}..N-1, en orden
 * @param next        pronóstico del periodo siguiente al último dato (N)
 */
public record Forecast(int firstPeriod, List<Double> fitted, double next) {

	public Forecast {
		fitted = List.copyOf(fitted);
	}

	/** Pronóstico del periodo {@code period}; solo para {@code firstPeriod <= period < N}. */
	public double at(int period) {
		return fitted.get(period - firstPeriod);
	}

	/** Pares real-pronosticado desde {@code fromPeriod} (incluido) hasta el último dato. */
	public List<ForecastPair> pairs(List<Double> actuals, int fromPeriod) {
		if (fromPeriod < firstPeriod) {
			throw new IllegalArgumentException("El periodo " + fromPeriod + " no tiene pronóstico");
		}
		return IntStream.range(fromPeriod, actuals.size())
				.mapToObj(period -> new ForecastPair(actuals.get(period), at(period)))
				.toList();
	}

	/** Primer periodo en que todos los métodos comparados tienen pronóstico (ventana común, ADR 07). */
	public static int commonEvaluationStart(Collection<Forecast> forecasts) {
		return forecasts.stream().mapToInt(Forecast::firstPeriod).max().orElseThrow();
	}
}
