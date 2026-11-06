package co.sierraalta.production.indicators.application;

import java.math.BigDecimal;
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
	 * @param evaluationFrom primer periodo evaluado en todos los métodos; nulo si no hubo ninguno
	 * @param evaluationTo   último periodo evaluado; nulo si no hubo ninguno
	 * @param mapeThreshold  umbral vigente al ejecutar, como fracción; nulo en ejecuciones anteriores a él
	 */
	record StoredIndicators(Long executionId, Long seriesId, YearMonth evaluationFrom, YearMonth evaluationTo,
			BigDecimal mapeThreshold, List<MethodIndicators> methods) {
	}
}
