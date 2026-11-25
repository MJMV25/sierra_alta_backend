package co.sierraalta.production.forecast.application;

import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.history.domain.Measure;
import co.sierraalta.production.history.domain.Periodicity;
import co.sierraalta.production.indicators.application.ExecutionComparison;

/**
 * Recálculo sin guardar: lo que la hoja muestra en vivo mientras se cambian parámetros o datos (ADR 13).
 * Mismos cálculos que una ejecución guardada, con su tabla comparativa.
 *
 * @param dataVersion versión de datos de la serie; nula si se calculó con datos que aún no se guardan
 * @param draft       verdadero si se usaron los datos enviados en lugar de los guardados
 */
public record ForecastPreview(Long seriesId, String productCode, String productName, Measure measure,
		Periodicity periodicity, String unit, Long dataVersion, boolean draft, YearMonth periodFrom,
		YearMonth periodTo, YearMonth evaluationStart, List<ForecastDetail.Actual> actuals,
		List<ForecastDetail.MethodResult> methods, ExecutionComparison comparison) {
}
