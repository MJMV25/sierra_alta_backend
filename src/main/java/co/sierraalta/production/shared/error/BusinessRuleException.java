package co.sierraalta.production.shared.error;

import java.io.Serial;

/** La solicitud es válida en forma pero viola una regla del negocio. HTTP 422. */
public class BusinessRuleException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public BusinessRuleException(String message) {
		super(message);
	}
}
