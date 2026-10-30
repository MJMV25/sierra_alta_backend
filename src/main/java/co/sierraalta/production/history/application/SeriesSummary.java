package co.sierraalta.production.history.application;

import java.time.YearMonth;

import co.sierraalta.production.history.domain.Measure;
import co.sierraalta.production.history.domain.Periodicity;

/**
 * Serie de un producto con el rango cargado. Sin datos, los periodos y {@code dataVersion} son nulos y
 * {@code observations} es 0.
 */
public record SeriesSummary(Long id, Measure measure, Periodicity periodicity, String unit, YearMonth firstPeriod,
		YearMonth lastPeriod, long observations, Long dataVersion) {
}
