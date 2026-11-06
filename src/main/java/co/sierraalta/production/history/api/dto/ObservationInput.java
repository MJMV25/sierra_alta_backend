package co.sierraalta.production.history.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/** Un mes de producción. Los límites del valor corresponden a la columna {@code NUMERIC(18, 4)}. */
public record ObservationInput(
		@NotNull(message = "El periodo es obligatorio.")
		@Pattern(regexp = "^(19|20)\\d{2}-(0[1-9]|1[0-2])$", message = "Use el formato AAAA-MM, por ejemplo 2026-10.")
		String period,

		@NotNull(message = "La cantidad es obligatoria; si no hubo producción, escriba 0.")
		@PositiveOrZero(message = "La cantidad no puede ser negativa.")
		@Digits(integer = 14, fraction = 4, message = "Use como máximo 14 dígitos enteros y 4 decimales.")
		BigDecimal value) {
}
