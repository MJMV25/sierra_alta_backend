package co.sierraalta.production.forecast.domain;

/** Métodos disponibles. El código se guarda tal cual en {@code metodo_ejecutado.metodo}. */
public enum MethodCode {
	SIMPLE_MOVING_AVERAGE,
	WEIGHTED_MOVING_AVERAGE,
	EXPONENTIAL_SMOOTHING,
	/** Holt. */
	TREND_ADJUSTED_EXPONENTIAL_SMOOTHING,
	/** Regresión lineal sobre t (sin parámetros). */
	LINEAR_REGRESSION,
	/** Estacional multiplicativo (parámetro L, longitud del ciclo). */
	SEASONAL_MULTIPLICATIVE
}
