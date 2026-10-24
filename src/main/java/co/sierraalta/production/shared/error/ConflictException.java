package co.sierraalta.production.shared.error;

import java.io.Serial;

/** La operación choca con el estado actual de los datos (por ejemplo, un nombre único repetido). HTTP 409. */
public class ConflictException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public ConflictException(String message) {
		super(message);
	}
}
