package co.sierraalta.production.auth.domain;

/**
 * Operaciones protegidas del sistema. Cada una se publica como una autoridad de Spring Security
 * y es la unidad con la que se autorizan los endpoints (privilegio mínimo).
 */
public enum Permission {

	/** Crear y desactivar cuentas, asignar rol y consultar eventos de seguridad. */
	USERS_MANAGE("users:manage"),
	/** Consultar productos, series y observaciones. */
	HISTORY_READ("history:read"),
	/** Cargar y validar históricos. */
	HISTORY_WRITE("history:write"),
	/** Ejecutar un pronóstico sobre una serie. */
	FORECAST_EXECUTE("forecast:execute"),
	/** Consultar ejecuciones y resultados de pronóstico. */
	FORECAST_READ("forecast:read"),
	/** Consultar indicadores de error (MAD, MAPE). */
	INDICATORS_READ("indicators:read");

	private final String authority;

	Permission(String authority) {
		this.authority = authority;
	}

	/** Nombre de la autoridad usada en las reglas de autorización y expuesta al frontend. */
	public String authority() {
		return authority;
	}
}
