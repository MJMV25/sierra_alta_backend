package co.sierraalta.production.shared.error;

import java.io.Serial;

/** El recurso solicitado no existe. Se traduce a HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
