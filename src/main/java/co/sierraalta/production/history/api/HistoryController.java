package co.sierraalta.production.history.api;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.auth.infrastructure.security.AccountPrincipal;
import co.sierraalta.production.history.api.dto.UploadObservationsRequest;
import co.sierraalta.production.history.application.HistoryService;
import co.sierraalta.production.history.application.LoadSummary;
import co.sierraalta.production.history.application.NewObservation;
import co.sierraalta.production.history.application.ProductSummary;
import co.sierraalta.production.history.application.SeriesObservations;
import co.sierraalta.production.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato REST del módulo de históricos. Permisos (ver {@code SecurityConfig}):
 * lectura {@code history:read} (planeador, supervisor, analista); carga {@code history:write} (analista).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/history")
public class HistoryController {

	private final HistoryService history;

	public HistoryController(HistoryService history) {
		this.history = history;
	}

	@GetMapping("/products")
	public List<ProductSummary> products() {
		return history.products();
	}

	@GetMapping("/series/{seriesId}/observations")
	public SeriesObservations observations(@PathVariable Long seriesId) {
		return history.observations(seriesId);
	}

	@PostMapping("/series/{seriesId}/observations")
	public ResponseEntity<LoadSummary> uploadObservations(@PathVariable Long seriesId,
			@Valid @RequestBody UploadObservationsRequest body, @AuthenticationPrincipal AccountPrincipal actor) {
		List<NewObservation> batch = body.observations().stream()
				.map(o -> new NewObservation(YearMonth.parse(o.period()), o.value()))
				.toList();
		LoadSummary load = history.load(seriesId, batch, actor.id());
		return ResponseEntity.created(URI.create(ApiPaths.V1 + "/history/series/" + seriesId + "/observations"))
				.body(load);
	}
}
