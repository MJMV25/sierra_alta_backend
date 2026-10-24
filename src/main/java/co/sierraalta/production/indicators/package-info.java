/**
 * Módulo de indicadores: calcula MAD y MAPE a partir de pares real-pronosticado.
 *
 * <p>MAD es el promedio de errores absolutos. MAPE promedia |error| / real en porcentaje sobre los pares
 * con real distinto de cero; informa cuántos pares usó y excluyó y, si no queda ninguno, indica que no
 * es calculable en lugar de devolver cero. Las fórmulas viven solo aquí (DRY): ni el frontend ni
 * {@code forecast} las duplican.
 *
 * <p>Estado: esquema creado (tabla {@code metrica}) y endpoint protegido por rol; pendiente de implementación.
 */
package co.sierraalta.production.indicators;
