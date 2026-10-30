package co.sierraalta.production.indicators.domain;

/** Valor real de un periodo y el pronóstico que se había hecho para ese mismo periodo. */
public record ForecastPair(double actual, double forecast) {

	public double absoluteError() {
		return Math.abs(actual - forecast);
	}
}
