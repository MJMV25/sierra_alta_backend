/**
 * Módulo de históricos: productos, series y observaciones.
 *
 * <p>Responsabilidades: validar los datos cargados (lote completo en una transacción, valores no
 * negativos, un dato por serie y periodo, periodos mensuales normalizados al primer día del mes,
 * un dato ausente no se convierte en cero) y entregar series ordenadas.
 *
 * <p>Expone un servicio público que el módulo {@code forecast} usará para consultar series. Ningún
 * otro módulo accede directamente a sus tablas ({@code producto}, {@code serie}, {@code observacion}).
 *
 * <p>Estado: esquema creado (migración V2) y endpoints protegidos por rol; la lógica se implementa
 * cuando Industrial valide el formato de carga.
 */
package co.sierraalta.production.history;
