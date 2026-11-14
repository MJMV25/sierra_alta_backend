package co.sierraalta.production.history.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Un mes de la hoja: unidades producidas, enteras y no negativas (ADR 13). {@code value} nulo borra el mes.
 */
public record MonthChangeInput(
		@NotNull(message = "El periodo es obligatorio.")
		@Pattern(regexp = "^(19|20)\\d{2}-(0[1-9]|1[0-2])$", message = "Use el formato AAAA-MM, por ejemplo 2026-10.")
		String period,

		@PositiveOrZero(message = "La cantidad no puede ser negativa.")
		@Digits(integer = 14, fraction = 0, message = "Escriba unidades enteras, de hasta 14 dígitos.")
		BigDecimal value) {
}
