package co.sierraalta.production.forecast.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/** Un dato real enviado para la vista previa. Mismos límites que la columna {@code NUMERIC(18, 4)}. */
public record ActualInput(
		@NotNull(message = "El periodo es obligatorio.")
		@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Use el formato AAAA-MM.")
		String period,

		@NotNull(message = "Escriba un valor; si no hubo producción, escriba 0.")
		@PositiveOrZero(message = "El valor no puede ser negativo.")
		@Digits(integer = 14, fraction = 4, message = "Use como máximo 14 dígitos enteros y 4 decimales.")
		BigDecimal value) {
}
