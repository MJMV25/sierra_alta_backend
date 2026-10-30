package co.sierraalta.production.forecast.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Serie a pronosticar y de 1 a 4 métodos distintos que se comparan en la misma ventana. */
public record CreateForecastRequest(
		@NotNull(message = "Indique la serie.")
		Long seriesId,

		@NotEmpty(message = "Elija al menos un método.")
		@Size(max = 4, message = "Elija como máximo los 4 métodos.")
		List<@Valid @NotNull(message = "El método no puede ser nulo.") MethodRequest> methods) {
}
