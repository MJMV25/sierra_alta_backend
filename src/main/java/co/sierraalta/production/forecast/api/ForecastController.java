package co.sierraalta.production.forecast.api;

import java.net.URI;
import java.util.List;

import co.sierraalta.production.auth.infrastructure.security.AccountPrincipal;
import co.sierraalta.production.forecast.api.dto.CreateForecastRequest;
import co.sierraalta.production.forecast.api.dto.MethodRequest;
import co.sierraalta.production.forecast.application.ExecutionOutcome;
import co.sierraalta.production.forecast.application.ForecastCommand;
import co.sierraalta.production.forecast.application.ForecastDetail;
import co.sierraalta.production.forecast.application.ForecastService;
import co.sierraalta.production.forecast.application.ForecastSummary;
import co.sierraalta.production.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato REST del módulo de pronósticos. Permisos (ver {@code SecurityConfig}):
 * ejecutar {@code forecast:execute} (planeador, analista); consultar {@code forecast:read}
 * (planeador, supervisor, analista).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/forecasts")
public class ForecastController {

	private final ForecastService forecasts;

	public ForecastController(ForecastService forecasts) {
		this.forecasts = forecasts;
	}

	/** 201 con la ejecución nueva; 200 con la original si se reenvía la misma {@code Idempotency-Key}. */
	@PostMapping
	public ResponseEntity<ForecastDetail> execute(@Valid @RequestBody CreateForecastRequest body,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
			@AuthenticationPrincipal AccountPrincipal actor) {
		ForecastCommand command = new ForecastCommand(body.seriesId(),
				body.methods().stream().map(MethodRequest::toSpec).toList());
		ExecutionOutcome outcome = forecasts.execute(command, actor.id(), idempotencyKey);
		if (!outcome.created()) {
			return ResponseEntity.ok(outcome.detail());
		}
		return ResponseEntity.created(URI.create(ApiPaths.V1 + "/forecasts/" + outcome.detail().id()))
				.body(outcome.detail());
	}

	@GetMapping
	public List<ForecastSummary> executions(@RequestParam(required = false) Long seriesId) {
		return forecasts.list(seriesId);
	}

	@GetMapping("/{executionId}")
	public ForecastDetail execution(@PathVariable Long executionId) {
		return forecasts.get(executionId);
	}
}
