package co.sierraalta.production.forecast.application;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.forecast.domain.MethodCode;
import co.sierraalta.production.history.domain.Periodicity;

/** Fila del listado de ejecuciones. */
public record ForecastSummary(Long id, Long seriesId, String productName, Periodicity periodicity,
		Instant executedAt, Long executedBy,
		Long dataVersion, YearMonth evaluationStart, List<MethodCode> methods) {
}
