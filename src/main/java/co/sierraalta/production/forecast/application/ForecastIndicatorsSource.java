package co.sierraalta.production.forecast.application;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import co.sierraalta.production.forecast.domain.MethodRun;
import co.sierraalta.production.forecast.domain.MetricCode;
import co.sierraalta.production.forecast.domain.MetricValue;
import co.sierraalta.production.forecast.infrastructure.persistence.ForecastExecutionRepository;
import co.sierraalta.production.indicators.application.ExecutionIndicatorsSource;
import co.sierraalta.production.indicators.domain.MethodComparison.MethodIndicators;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega a {@code indicators} los indicadores guardados de una ejecución, sin exponerle las entidades de
 * este módulo. La autorización la hace {@code IndicatorsService}, que es quien lo invoca.
 */
@Component
class ForecastIndicatorsSource implements ExecutionIndicatorsSource {

	private final ForecastExecutionRepository executions;

	ForecastIndicatorsSource(ForecastExecutionRepository executions) {
		this.executions = executions;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredIndicators> find(Long executionId) {
		return executions.findById(executionId).map(execution -> stored(execution.getId(), execution.getSeriesId(),
				execution.getEvaluationStart(), execution.getInputs().getLast().getPeriod(), execution.getMethods()));
	}

	/** También lo usa la vista previa, con {@code executionId} nulo. */
	static StoredIndicators stored(Long executionId, Long seriesId, YearMonth evaluationStart, YearMonth lastPeriod,
			List<MethodRun> runs) {
		return new StoredIndicators(executionId, seriesId, evaluationStart,
				evaluationStart == null ? null : lastPeriod,
				runs.stream().map(ForecastIndicatorsSource::indicators).toList());
	}

	private static MethodIndicators indicators(MethodRun run) {
		MetricValue mad = run.metric(MetricCode.MAD).orElseThrow();
		MetricValue mape = run.metric(MetricCode.MAPE).orElseThrow();
		return new MethodIndicators(run.getMethod().name(), mad.getValue(), mape.getValue(), mad.getPairsUsed(),
				mape.getPairsExcluded(), run.getMethod().comparable());
	}
}
