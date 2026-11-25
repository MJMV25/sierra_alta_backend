package co.sierraalta.production.history.infrastructure.persistence;

/** Id de la última carga de una serie: su versión de datos. */
public record LatestLoad(Long seriesId, Long loadId) {
}
