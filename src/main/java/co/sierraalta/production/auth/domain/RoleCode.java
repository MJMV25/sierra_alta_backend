package co.sierraalta.production.auth.domain;

import static co.sierraalta.production.auth.domain.Permission.FORECAST_EXECUTE;
import static co.sierraalta.production.auth.domain.Permission.FORECAST_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_WRITE;
import static co.sierraalta.production.auth.domain.Permission.INDICATORS_READ;
import static co.sierraalta.production.auth.domain.Permission.USERS_MANAGE;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Roles del sistema y su alcance (tabla 2 del documento de arquitectura).
 *
 * <p>El alcance vive en el código y no en tablas de permisos: el modelo asigna un único rol por usuario
 * y los permisos aún deben validarse con Industrial (YAGNI). Cambiar un alcance implica modificar este
 * enum y sus pruebas. El código debe coincidir con la columna {@code rol.codigo}.
 */
public enum RoleCode {

	/** Administra cuentas. No obtiene automáticamente permisos sobre históricos ni pronósticos. */
	ADMIN(EnumSet.of(USERS_MANAGE)),

	/** Consulta históricos, ejecuta pronósticos y consulta indicadores. */
	PLANNER(EnumSet.of(HISTORY_READ, FORECAST_EXECUTE, FORECAST_READ, INDICATORS_READ)),

	/** Consulta históricos y resultados. El filtro por línea queda pendiente de definición funcional. */
	SUPERVISOR(EnumSet.of(HISTORY_READ, FORECAST_READ, INDICATORS_READ)),

	/** Carga y valida históricos, ejecuta cálculos y revisa indicadores. */
	ANALYST(EnumSet.of(HISTORY_READ, HISTORY_WRITE, FORECAST_EXECUTE, FORECAST_READ, INDICATORS_READ));

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
