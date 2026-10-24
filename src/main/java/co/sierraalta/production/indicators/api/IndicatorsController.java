package co.sierraalta.production.indicators.api;

import co.sierraalta.production.shared.error.FeatureNotImplementedException;
import co.sierraalta.production.shared.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contrato REST del módulo de indicadores. Permiso {@code indicators:read} (planeador, supervisor, analista). */
@RestController
@RequestMapping(ApiPaths.V1 + "/indicators")
public class IndicatorsController {

	@GetMapping("/executions/{executionId}")
	public void metrics(@PathVariable Long executionId) {
		throw new FeatureNotImplementedException("indicadores de una ejecución");
	}
}
