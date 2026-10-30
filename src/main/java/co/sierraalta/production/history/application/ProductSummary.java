package co.sierraalta.production.history.application;

import java.util.List;

/** Referencia de producto activa con sus series. */
public record ProductSummary(Long id, String code, String name, List<SeriesSummary> series) {
}
