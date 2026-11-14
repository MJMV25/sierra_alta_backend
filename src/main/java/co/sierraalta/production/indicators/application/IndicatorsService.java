package co.sierraalta.production.indicators.application;

import co.sierraalta.production.shared.error.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Compara los métodos de una ejecución guardada con sus indicadores. */
@Service
public class IndicatorsService {

	private final ExecutionIndicatorsSource executions;

	public IndicatorsService(ExecutionIndicatorsSource executions) {
		this.executions = executions;
	}

	@PreAuthorize("hasAuthority('indicators:read')")
	public ExecutionComparison compare(Long executionId) {
		return ExecutionComparison.of(executions.find(executionId).orElseThrow(
				() -> new ResourceNotFoundException("No existe la ejecución con id " + executionId + ".")));
	}
}
