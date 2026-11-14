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
	/** Editar la hoja de datos: agregar, corregir y borrar meses (ADR 13). */
	HISTORY_WRITE("history:write"),
	/** Guardar un pronóstico (ejecución con trazabilidad). Recalcular sin guardar solo exige {@link #FORECAST_READ}. */
	FORECAST_EXECUTE("forecast:execute"),
	/** Consultar ejecuciones y resultados de pronóstico. */
	FORECAST_READ("forecast:read"),
	/** Consultar indicadores de error (MAD, MAPE). */
	INDICATORS_READ("indicators:read"),
	/** Descargar pronósticos, gráficos e indicadores (Excel, PDF, CSV, imagen); respuesta 23 de Industrial. */
	FORECAST_EXPORT("forecast:export");

	private final String authority;

	Permission(String authority) {
		this.authority = authority;
	}

	/** Nombre de la autoridad usada en las reglas de autorización y expuesta al frontend. */
	public String authority() {
		return authority;
	}
}
