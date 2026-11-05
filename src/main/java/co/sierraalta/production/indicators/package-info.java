/**
 * Módulo de indicadores: calcula MAD y MAPE a partir de pares real-pronosticado.
 *
 * <p>MAD es el promedio de errores absolutos. MAPE promedia |error| / real sobre los pares con real
 * distinto de cero (fracción; se muestra ×100); informa cuántos pares usó y excluyó y, si no queda
 * ninguno, indica que no es calculable en lugar de devolver cero. Las fórmulas viven solo aquí (DRY):
 * ni el frontend ni {@code forecast} las duplican.
 *
 * <p>{@code domain.MethodComparison} arma la tabla comparativa: el mejor método por MAD y por MAPE, sin
 * ganador único, y el estado de cada uno frente al umbral del MAPE (ADR 07, preguntas 21 y 22). Lee las
 * ejecuciones guardadas mediante el puerto {@code application.ExecutionIndicatorsSource}, que implementa
 * {@code forecast}: así este módulo nunca depende de aquel.
 *
 * <p>Estado: funcional (fase 3).
 */
package co.sierraalta.production.indicators;
