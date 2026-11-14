package co.sierraalta.production.forecast.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Serie a pronosticar, periodo histórico opcional ({@code from} y {@code to} como {@code AAAA-MM}; sin ellos se
 * usa toda la serie) y de 1 a 6 métodos distintos que se comparan en la misma ventana.
 */
public record CreateForecastRequest(
		@NotNull(message = "Indique la serie.")
		Long seriesId,

		@Pattern(regexp = MONTH, message = "Use el formato AAAA-MM.")
		String from,

		@Pattern(regexp = MONTH, message = "Use el formato AAAA-MM.")
		String to,

		@NotEmpty(message = "Elija al menos un método.")
		@Size(max = 6, message = "Elija como máximo los 6 métodos.")
		List<@Valid @NotNull(message = "El método no puede ser nulo.") MethodRequest> methods) {

	private static final String MONTH = "^\\d{4}-(0[1-9]|1[0-2])$";
}
