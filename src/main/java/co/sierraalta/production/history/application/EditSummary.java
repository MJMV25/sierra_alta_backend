package co.sierraalta.production.history.application;

/**
 * Resultado de guardar los cambios de una serie. {@code dataVersion} es la versión nueva (o la misma si
 * nada cambió de verdad).
 */
public record EditSummary(Long seriesId, Long dataVersion, int added, int corrected, int removed) {
}
