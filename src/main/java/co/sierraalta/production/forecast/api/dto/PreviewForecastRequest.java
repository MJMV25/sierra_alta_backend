package co.sierraalta.production.forecast.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Recálculo sin guardar (ADR 13): lo mismo que {@link CreateForecastRequest} más {@code actuals}, opcional,
 * con los datos de la hoja que aún no se guardan. Sin {@code actuals} se usan los datos guardados.
 */
public record PreviewForecastRequest(
		@NotNull(message = "Indique la serie.")
		Long seriesId,

		@Pattern(regexp = MONTH, message = "Use el formato AAAA-MM.")
		String from,

		@Pattern(regexp = MONTH, message = "Use el formato AAAA-MM.")
		String to,

		@NotEmpty(message = "Elija al menos un método.")
		@Size(max = 6, message = "Elija como máximo los 6 métodos.")
		List<@Valid @NotNull(message = "El método no puede ser nulo.") MethodRequest> methods,

		@Size(max = 1200, message = "Envíe como máximo 1200 datos.")
		List<@Valid @NotNull(message = "El dato no puede ser nulo.") ActualInput> actuals) {

	private static final String MONTH = "^\\d{4}-(0[1-9]|1[0-2])$";
}
