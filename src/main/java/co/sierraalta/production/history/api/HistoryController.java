package co.sierraalta.production.history.api;

import co.sierraalta.production.shared.error.FeatureNotImplementedException;
import co.sierraalta.production.shared.web.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato REST del módulo de históricos. Permisos (ver {@code SecurityConfig}):
 * lectura {@code history:read} (planeador, supervisor, analista); carga {@code history:write} (analista).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/history")
public class HistoryController {

	@GetMapping("/products")
	public void products() {
		throw new FeatureNotImplementedException("consulta de productos");
	}

	@GetMapping("/series/{seriesId}/observations")
	public void observations(@PathVariable Long seriesId) {
		throw new FeatureNotImplementedException("consulta de observaciones de una serie");
	}

	@PostMapping("/series/{seriesId}/observations")
	public void uploadObservations(@PathVariable Long seriesId) {
		throw new FeatureNotImplementedException("carga y validación de históricos");
	}
}
