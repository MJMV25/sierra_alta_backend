package co.sierraalta.production.indicators.api;

import co.sierraalta.production.indicators.application.ExecutionComparison;
import co.sierraalta.production.indicators.application.IndicatorsService;
import co.sierraalta.production.shared.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contrato REST del módulo de indicadores. Permiso {@code indicators:read} (administrador, planeador, analista). */
@RestController
@RequestMapping(ApiPaths.V1 + "/indicators")
public class IndicatorsController {

	private final IndicatorsService indicators;

	public IndicatorsController(IndicatorsService indicators) {
		this.indicators = indicators;
	}

	/** Tabla comparativa de los métodos de una ejecución: MAD, MAPE, mejores y estado frente al umbral. */
	@GetMapping("/executions/{executionId}")
	public ExecutionComparison comparison(@PathVariable Long executionId) {
		return indicators.compare(executionId);
	}
}
