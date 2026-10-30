package co.sierraalta.production.forecast.api.dto;

import java.math.BigDecimal;
import java.util.List;

import co.sierraalta.production.forecast.application.MethodSpec;
import co.sierraalta.production.forecast.domain.MethodCode;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Un método y sus parámetros. Aquí solo se valida la forma; los rangos (α y β entre 0 y 1, pesos que suman
 * 1, n no mayor que los datos) los valida el dominio y vuelven como error del mismo campo.
 */
public record MethodRequest(
		@NotNull(message = "Indique el método.")
		MethodCode method,

		@Max(value = 240, message = "n no puede superar 240 meses.")
		Integer periods,

		@Size(max = 240, message = "Indique como máximo 240 pesos.")
		List<@NotNull(message = "El peso no puede ser nulo.")
				@Digits(integer = 1, fraction = 10, message = "Use como máximo 10 decimales.") BigDecimal> weights,

		@Digits(integer = 1, fraction = 10, message = "Use como máximo 10 decimales.")
		BigDecimal alpha,

		@Digits(integer = 1, fraction = 10, message = "Use como máximo 10 decimales.")
		BigDecimal beta) {

	public MethodSpec toSpec() {
		return new MethodSpec(method, periods, weights, alpha, beta);
	}
}
