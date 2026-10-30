package co.sierraalta.production.history.application;

import java.time.Instant;
import java.time.YearMonth;

/** Resultado de una carga aceptada. {@code loadId} es la nueva versión de datos de la serie. */
public record LoadSummary(Long loadId, Long seriesId, int observations, YearMonth firstPeriod, YearMonth lastPeriod,
		Instant loadedAt) {
}
