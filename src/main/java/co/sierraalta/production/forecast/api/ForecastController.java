package co.sierraalta.production.forecast.api;

import co.sierraalta.production.shared.error.FeatureNotImplementedException;
import co.sierraalta.production.shared.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato REST del módulo de pronósticos. Permisos (ver {@code SecurityConfig}):
 * ejecutar {@code forecast:execute} (planeador, analista); consultar {@code forecast:read}
 * (planeador, supervisor, analista).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/forecasts")
public class ForecastController {

	@PostMapping
	public void execute() {
		throw new FeatureNotImplementedException("ejecución de pronóstico");
	}

	@GetMapping
	public void executions() {
		throw new FeatureNotImplementedException("consulta de ejecuciones");
	}

	@GetMapping("/{executionId}")
	public void execution(@PathVariable Long executionId) {
		throw new FeatureNotImplementedException("detalle de una ejecución");
	}
}
