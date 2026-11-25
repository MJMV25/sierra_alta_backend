package co.sierraalta.production.history.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cambios de la hoja de datos: una o varias series, que se guardan todas o ninguna. */
public record SaveSheetRequest(
		@NotEmpty(message = "Incluya al menos una serie.")
		@Size(max = 50, message = "Un guardado admite como máximo 50 series.")
		List<@Valid @NotNull(message = "La serie no puede ser nula.") SeriesChangesInput> series) {
}
