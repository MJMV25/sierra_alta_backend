package co.sierraalta.production.forecast.application;

/** Resultado de pedir una ejecución: {@code created} es falso si se reenvió una clave de idempotencia ya usada. */
public record ExecutionOutcome(ForecastDetail detail, boolean created) {
}
