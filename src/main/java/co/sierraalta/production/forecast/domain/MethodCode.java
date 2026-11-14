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
	SEASONAL_MULTIPLICATIVE;

	/**
	 * Si compite por ser el mejor método. El estacional multiplicativo no: igual que en el Excel, cada periodo
	 * usa el promedio de su propio ciclo, que incluye ese mismo dato, así que su error no es comparable con el
	 * de los métodos que solo usan datos pasados (ADR 12 y 13).
	 */
	public boolean comparable() {
		return this != SEASONAL_MULTIPLICATIVE;
	}
}
