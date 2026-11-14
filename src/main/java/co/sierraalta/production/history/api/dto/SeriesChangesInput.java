package co.sierraalta.production.history.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cambios de una serie. {@code dataVersion} es la versión que tenía la hoja al abrirse (opcional): si la serie
 * cambió después, se responde 409. 600 meses son 50 años.
 */
public record SeriesChangesInput(
		@NotNull(message = "Indique la serie.")
		Long seriesId,

		Long dataVersion,

		@NotEmpty(message = "Incluya al menos un mes.")
		@Size(max = SeriesChangesInput.MAX_MONTHS,
				message = "Un guardado admite como máximo " + SeriesChangesInput.MAX_MONTHS + " meses por serie.")
		List<@Valid @NotNull(message = "El mes no puede ser nulo.") MonthChangeInput> changes) {

	public static final int MAX_MONTHS = 600;
}
