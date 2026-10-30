/**
 * Módulo de indicadores: calcula MAD y MAPE a partir de pares real-pronosticado.
 *
 * <p>MAD es el promedio de errores absolutos. MAPE promedia |error| / real sobre los pares con real
 * distinto de cero (fracción; se muestra ×100); informa cuántos pares usó y excluyó y, si no queda
 * ninguno, indica que no es calculable en lugar de devolver cero. Las fórmulas viven solo aquí (DRY):
 * ni el frontend ni {@code forecast} las duplican.
 *
 * <p>Estado: cálculo implementado en {@code domain.ErrorIndicators} (ADR 07); pendiente el endpoint.
 */
package co.sierraalta.production.indicators;
