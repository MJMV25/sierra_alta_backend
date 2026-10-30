/**
 * Módulo de históricos: productos, series y observaciones.
 *
 * <p>Responsabilidades: validar los datos cargados (lote completo en una transacción, valores no
 * negativos, un dato por serie y periodo, periodos mensuales normalizados al primer día del mes, meses
 * consecutivos sin huecos, un dato ausente no se convierte en cero), registrar cada carga y entregar
 * series ordenadas.
 *
 * <p>Expone un servicio público que el módulo {@code forecast} usará para consultar series. Ningún
 * otro módulo accede directamente a sus tablas ({@code producto}, {@code serie}, {@code observacion}).
 *
 * <p>Estado: funcional (fase 1). {@code HistoryService} es el servicio público; reglas de carga en
 * {@code domain.MonthlyLoadRules} (ADR 07).
 */
package co.sierraalta.production.history;
