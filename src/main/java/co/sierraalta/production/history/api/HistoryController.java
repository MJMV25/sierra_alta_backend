package co.sierraalta.production.history.api;

import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.auth.infrastructure.security.AccountPrincipal;
import co.sierraalta.production.history.api.dto.SaveSheetRequest;
import co.sierraalta.production.history.api.dto.SeriesChangesInput;
import co.sierraalta.production.history.application.ChangeEntry;
import co.sierraalta.production.history.application.EditSummary;
import co.sierraalta.production.history.application.HistoryService;
import co.sierraalta.production.history.application.ProductSummary;
import co.sierraalta.production.history.application.SeriesEdit;
import co.sierraalta.production.history.application.SeriesObservations;
import co.sierraalta.production.history.domain.ObservationChange;
import co.sierraalta.production.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato REST del módulo de históricos. Permisos (ver {@code SecurityConfig}):
 * lectura {@code history:read} (todos los roles); editar la hoja {@code history:write} (solo administrador).
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

	@GetMapping("/series/{seriesId}/changes")
	public List<ChangeEntry> changes(@PathVariable Long seriesId) {
		return history.changes(seriesId);
	}

	/** Agrega, corrige o borra meses de una o varias series en una sola operación. */
	@PatchMapping("/observations")
	public List<EditSummary> saveSheet(@Valid @RequestBody SaveSheetRequest body,
			@AuthenticationPrincipal AccountPrincipal actor) {
		return history.edit(body.series().stream().map(HistoryController::edit).toList(), actor.id());
	}

	private static SeriesEdit edit(SeriesChangesInput input) {
		return new SeriesEdit(input.seriesId(), input.dataVersion(), input.changes().stream()
				.map(change -> new ObservationChange(YearMonth.parse(change.period()), change.value()))
				.toList());
	}
}
