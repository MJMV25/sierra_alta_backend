package co.sierraalta.production.auth.application;

import java.time.Instant;

import co.sierraalta.production.auth.domain.RoleCode;
import co.sierraalta.production.auth.domain.UserAccount;

/** Vista de una cuenta para administración. No expone el hash de la contraseña. */
public record UserSummary(Long id, String username, RoleCode role, String roleName, boolean active, Instant createdAt) {

	static UserSummary from(UserAccount user) {
		return new UserSummary(user.getId(), user.getUsername(), user.getRole().getCode(), user.getRole().getName(),
				user.isActive(), user.getCreatedAt());
	}
}
