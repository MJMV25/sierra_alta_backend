package co.sierraalta.production.history.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Lote de meses para una serie. 240 meses (20 años) cubre de sobra la carga inicial de 2022 a 2026. */
public record UploadObservationsRequest(
		@NotEmpty(message = "Incluya al menos un mes.")
		@Size(max = UploadObservationsRequest.MAX_OBSERVATIONS,
				message = "Una carga admite como máximo " + UploadObservationsRequest.MAX_OBSERVATIONS + " meses.")
		List<@Valid @NotNull(message = "El mes no puede ser nulo.") ObservationInput> observations) {

	public static final int MAX_OBSERVATIONS = 240;
}
