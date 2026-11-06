package co.sierraalta.production.shared.error;

import java.io.Serial;

/**
 * Un dato de la solicitud es inválido y solo puede comprobarse en el caso de uso (por ejemplo, los pesos de
 * un método que deben sumar 1). Se traduce a HTTP 400 {@code validation}, igual que Bean Validation, para
 * mostrar el mensaje junto al campo {@code field} del formulario.
 */
public class InvalidFieldException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String field;

	public InvalidFieldException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
