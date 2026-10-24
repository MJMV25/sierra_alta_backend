package co.sierraalta.production.auth.application;

import java.util.List;

import co.sierraalta.production.auth.domain.Permission;
import co.sierraalta.production.auth.domain.Role;
import co.sierraalta.production.auth.domain.RoleCode;

/** Rol del catálogo con el alcance (permisos) que concede. */
public record RoleSummary(RoleCode code, String name, List<String> permissions) {

	static RoleSummary from(Role role) {
		List<String> permissions = role.getCode().permissions().stream().map(Permission::authority).toList();
		return new RoleSummary(role.getCode(), role.getName(), permissions);
	}
}
