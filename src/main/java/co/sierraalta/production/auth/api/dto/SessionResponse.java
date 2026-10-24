package co.sierraalta.production.auth.api.dto;

import java.util.List;

import co.sierraalta.production.auth.domain.Permission;
import co.sierraalta.production.auth.domain.RoleCode;
import co.sierraalta.production.auth.infrastructure.security.AccountPrincipal;

/**
 * Sesión actual. El frontend usa {@code permissions} solo para mostrar u ocultar opciones;
 * la autoridad de seguridad sigue siendo el servidor.
 */
public record SessionResponse(Long userId, String username, RoleCode role, String roleName,
		List<String> permissions) {

	public static SessionResponse from(AccountPrincipal principal) {
		List<String> permissions = principal.permissions().stream().map(Permission::authority).toList();
		return new SessionResponse(principal.id(), principal.getUsername(), principal.role(), principal.roleName(),
				permissions);
	}
}
