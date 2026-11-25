package co.sierraalta.production.history.infrastructure.persistence;

import java.time.LocalDate;

/** Resumen de las observaciones de una serie: primer y último mes y cuántos meses tiene. */
public record SeriesStats(Long seriesId, LocalDate firstPeriod, LocalDate lastPeriod, long count) {
}
