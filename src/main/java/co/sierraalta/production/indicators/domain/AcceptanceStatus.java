package co.sierraalta.production.indicators.domain;

/** Resultado de comparar el MAPE de un método con el umbral de aceptación (ADR 07, pregunta 21). */
public enum AcceptanceStatus {

	/** MAPE menor que el umbral. */
	CUMPLE,
	/** MAPE mayor o igual que el umbral. */
	NO_CUMPLE,
	/** El MAPE no es calculable (sin pares, o todos con real 0). */
	SIN_EVALUAR,
	/** La ejecución se hizo antes de fijar un umbral. */
	SIN_CRITERIO
}
