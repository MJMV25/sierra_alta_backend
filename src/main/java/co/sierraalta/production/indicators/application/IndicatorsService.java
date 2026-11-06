package co.sierraalta.production.indicators.application;

import java.math.BigDecimal;
import java.util.List;

import co.sierraalta.production.indicators.application.ExecutionComparison.MethodRow;
import co.sierraalta.production.indicators.application.ExecutionIndicatorsSource.StoredIndicators;
import co.sierraalta.production.indicators.domain.MethodComparison;
import co.sierraalta.production.indicators.domain.MethodComparison.Row;
import co.sierraalta.production.shared.Decimals;
import co.sierraalta.production.shared.error.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Compara los métodos de una ejecución guardada con sus indicadores y el umbral de aceptación. */
@Service
public class IndicatorsService {

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final ExecutionIndicatorsSource executions;

	public IndicatorsService(ExecutionIndicatorsSource executions) {
		this.executions = executions;
	}

	@PreAuthorize("hasAuthority('indicators:read')")
	public ExecutionComparison compare(Long executionId) {
		StoredIndicators stored = executions.find(executionId).orElseThrow(
				() -> new ResourceNotFoundException("No existe la ejecución con id " + executionId + "."));
		List<Row> rows = MethodComparison.of(stored.mapeThreshold(), stored.methods());
		return new ExecutionComparison(stored.executionId(), stored.seriesId(), stored.evaluationFrom(),
				stored.evaluationTo(), percent(stored.mapeThreshold()),
				rows.stream().filter(Row::bestByMad).map(row -> row.indicators().method()).toList(),
				rows.stream().filter(Row::bestByMape).map(row -> row.indicators().method()).toList(),
				rows.stream().map(IndicatorsService::methodRow).toList());
	}

	private static MethodRow methodRow(Row row) {
		var m = row.indicators();
		return new MethodRow(m.method(), Decimals.plain(m.mad()), percent(m.mape()), m.pairsUsed(), m.mapeExcluded(),
				row.bestByMad(), row.bestByMape(), row.status());
	}

	private static BigDecimal percent(BigDecimal fraction) {
		return fraction == null ? null : Decimals.plain(fraction.multiply(HUNDRED));
	}
}
