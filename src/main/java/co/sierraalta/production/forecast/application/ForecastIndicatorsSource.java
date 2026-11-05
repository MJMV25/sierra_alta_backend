package co.sierraalta.production.forecast.application;

import java.time.YearMonth;
import java.util.Optional;

import co.sierraalta.production.forecast.domain.ForecastExecution;
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
		return executions.findById(executionId).map(ForecastIndicatorsSource::stored);
	}

	private static StoredIndicators stored(ForecastExecution execution) {
		YearMonth from = execution.getEvaluationStart();
		YearMonth to = from == null ? null : execution.getInputs().getLast().getPeriod();
		return new StoredIndicators(execution.getId(), execution.getSeriesId(), from, to,
				execution.getMapeThreshold(),
				execution.getMethods().stream().map(ForecastIndicatorsSource::indicators).toList());
	}

	private static MethodIndicators indicators(MethodRun run) {
		MetricValue mad = run.metric(MetricCode.MAD).orElseThrow();
		MetricValue mape = run.metric(MetricCode.MAPE).orElseThrow();
		return new MethodIndicators(run.getMethod().name(), mad.getValue(), mape.getValue(), mad.getPairsUsed(),
				mape.getPairsExcluded());
	}
}
