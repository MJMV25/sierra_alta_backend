package co.sierraalta.production.forecast.application;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.forecast.domain.MethodCode;

/** Fila del listado de ejecuciones. */
public record ForecastSummary(Long id, Long seriesId, String productName, Instant executedAt, Long executedBy,
		Long dataVersion, YearMonth evaluationStart, List<MethodCode> methods) {
}
