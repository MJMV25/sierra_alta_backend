package co.sierraalta.production.auth.domain;

/** Tipos de evento auditados por el módulo de autenticación. */
public enum SecurityEventType {
	LOGIN,
	LOGOUT,
	ACCESS_DENIED,
	/** Solicitud que modifica estado sin token CSRF válido. */
	CSRF_REJECTED,
	USER_CREATED,
	USER_ACTIVATED,
	USER_DEACTIVATED,
	ROLE_CHANGED
}
