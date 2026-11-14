package co.sierraalta.production.history.application;

import java.util.List;

/**
 * Referencia de producto activa con sus series. {@code sample}: datos de ejemplo del Excel (Spokane, ACME),
 * de solo lectura.
 */
public record ProductSummary(Long id, String code, String name, boolean sample, List<SeriesSummary> series) {
}
