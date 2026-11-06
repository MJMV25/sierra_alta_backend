package co.sierraalta.production.history.infrastructure.persistence;

import java.time.LocalDate;

/** Resumen de las observaciones de una serie; {@code dataVersion} es el id de su última carga. */
public record SeriesStats(Long seriesId, LocalDate firstPeriod, LocalDate lastPeriod, long count, Long dataVersion) {
}
