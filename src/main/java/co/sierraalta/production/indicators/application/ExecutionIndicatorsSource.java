package co.sierraalta.production.indicators.application;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import co.sierraalta.production.indicators.domain.MethodComparison.MethodIndicators;

/**
 * Puerto con que {@code indicators} lee los indicadores guardados de una ejecución. Lo implementa
 * {@code forecast}, dueño de las tablas de ejecuciones (ADR 01): así este módulo no depende de aquel y no
 * se forma un ciclo, porque {@code forecast} ya usa {@code indicators} para calcular MAD y MAPE.
 */
public interface ExecutionIndicatorsSource {

	Optional<StoredIndicators> find(Long executionId);

	/**
	 * @param executionId    nulo en un recálculo sin guardar (vista previa)
	 * @param evaluationFrom primer periodo evaluado en todos los métodos; nulo si no hubo ninguno
	 * @param evaluationTo   último periodo evaluado; nulo si no hubo ninguno
	 */
	record StoredIndicators(Long executionId, Long seriesId, YearMonth evaluationFrom, YearMonth evaluationTo,
			List<MethodIndicators> methods) {
	}
}
