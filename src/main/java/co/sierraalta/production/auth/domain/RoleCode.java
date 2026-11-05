package co.sierraalta.production.auth.domain;

import static co.sierraalta.production.auth.domain.Permission.FORECAST_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_WRITE;
import static co.sierraalta.production.auth.domain.Permission.INDICATORS_READ;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Roles del sistema y su alcance (tabla 2 del documento de arquitectura, ajustada el 2026-11-05: sin
 * supervisor y con la ejecución de pronósticos solo para el administrador; ADR 05).
 *
 * <p>El alcance vive en el código y no en tablas de permisos: el modelo asigna un único rol por usuario
 * y los permisos aún deben validarse con Industrial (YAGNI). Cambiar un alcance implica modificar este
 * enum y sus pruebas. El código debe coincidir con la columna {@code rol.codigo}.
 */
public enum RoleCode {

	/** Tiene todos los permisos; es el único que ejecuta pronósticos y administra cuentas. */
	ADMIN(EnumSet.allOf(Permission.class)),

	/** Consulta históricos, pronósticos e indicadores; es quien extrae la información de los pronósticos. */
	PLANNER(EnumSet.of(HISTORY_READ, FORECAST_READ, INDICATORS_READ)),

	/** Carga y valida históricos y consulta pronósticos e indicadores. No ejecuta pronósticos. */
	ANALYST(EnumSet.of(HISTORY_READ, HISTORY_WRITE, FORECAST_READ, INDICATORS_READ));

	private final Set<Permission> permissions;

	RoleCode(EnumSet<Permission> permissions) {
		this.permissions = Collections.unmodifiableSet(permissions);
	}

	public Set<Permission> permissions() {
		return permissions;
	}

	public boolean grants(Permission permission) {
		return permissions.contains(permission);
	}
}
