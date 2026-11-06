package co.sierraalta.production.forecast.domain;

import java.io.Serial;

/** Un parámetro del método o la serie no cumple las reglas del ADR 07. {@code field} nombra el dato. */
public class InvalidForecastParameterException extends IllegalArgumentException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String field;

	public InvalidForecastParameterException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
