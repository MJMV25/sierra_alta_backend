package co.sierraalta.production.auth.application;

import java.util.List;

import co.sierraalta.production.auth.domain.RoleCode;
import co.sierraalta.production.auth.domain.SecurityEventResult;
import co.sierraalta.production.auth.domain.SecurityEventType;
import co.sierraalta.production.auth.domain.UserAccount;
import co.sierraalta.production.auth.domain.Usernames;
import co.sierraalta.production.auth.infrastructure.persistence.RoleRepository;
import co.sierraalta.production.auth.infrastructure.persistence.SecurityEventRepository;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import co.sierraalta.production.shared.error.BusinessRuleException;
import co.sierraalta.production.shared.error.ConflictException;
import co.sierraalta.production.shared.error.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del administrador del sistema: crear y desactivar cuentas y asignar el rol.
 *
 * <p>Protege la operación del sistema: nadie puede desactivarse o cambiarse el rol a sí mismo y
 * siempre debe quedar al menos un administrador activo.
 *
 * <p>Exige el permiso {@code users:manage} aunque la ruta ya lo verifique en {@code SecurityConfig}:
 * el caso de uso queda protegido si se invoca desde otro punto de entrada.
 */
@Service
@Transactional
@PreAuthorize("hasAuthority('users:manage')")
public class UserAdministrationService {

	private final UserAccountRepository users;
	private final RoleRepository roles;
	private final SecurityEventRepository events;
	private final PasswordEncoder passwordEncoder;
	private final SecurityEventRecorder eventRecorder;

	public UserAdministrationService(UserAccountRepository users, RoleRepository roles,
			SecurityEventRepository events, PasswordEncoder passwordEncoder, SecurityEventRecorder eventRecorder) {
		this.users = users;
		this.roles = roles;
		this.events = events;
		this.passwordEncoder = passwordEncoder;
		this.eventRecorder = eventRecorder;
	}

	@Transactional(readOnly = true)
	public List<UserSummary> listUsers() {
		return users.findAllByOrderByUsernameAsc().stream().map(UserSummary::from).toList();
	}

	@Transactional(readOnly = true)
	public List<RoleSummary> listRoles() {
		return roles.findAll().stream().map(RoleSummary::from).toList();
	}

	@Transactional(readOnly = true)
	public List<SecurityEventSummary> recentEvents(int limit) {
		int size = Math.clamp(limit, 1, 500);
		return events.findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, size)).stream()
				.map(SecurityEventSummary::from)
				.toList();
	}

	public UserSummary createUser(String username, String rawPassword, RoleCode roleCode, Long actorId) {
		String normalized = Usernames.normalize(username);
		if (users.existsByUsername(normalized)) {
			throw new ConflictException("Ya existe una cuenta con el nombre de usuario '" + normalized + "'.");
		}
		UserAccount user = users.save(
				UserAccount.create(normalized, passwordEncoder.encode(rawPassword), roles.getByCode(roleCode)));
		eventRecorder.record(actorId, SecurityEventType.USER_CREATED, SecurityEventResult.SUCCESS);
		return UserSummary.from(user);
	}

	public UserSummary changeRole(Long userId, RoleCode newRole, Long actorId) {
		UserAccount user = user(userId);
		if (user.hasRole(newRole)) {
			return UserSummary.from(user);
		}
		if (userId.equals(actorId)) {
			throw new BusinessRuleException("No puede cambiar el rol de su propia cuenta.");
		}
		ensureAnotherActiveAdminRemains(user);
		user.assignRole(roles.getByCode(newRole));
		eventRecorder.record(actorId, SecurityEventType.ROLE_CHANGED, SecurityEventResult.SUCCESS);
		return UserSummary.from(user);
	}

	public UserSummary changeStatus(Long userId, boolean active, Long actorId) {
		UserAccount user = user(userId);
		if (user.isActive() == active) {
			return UserSummary.from(user);
		}
		if (active) {
			user.activate();
			eventRecorder.record(actorId, SecurityEventType.USER_ACTIVATED, SecurityEventResult.SUCCESS);
		}
		else {
			if (userId.equals(actorId)) {
				throw new BusinessRuleException("No puede desactivar su propia cuenta.");
			}
			ensureAnotherActiveAdminRemains(user);
			user.deactivate();
			eventRecorder.record(actorId, SecurityEventType.USER_DEACTIVATED, SecurityEventResult.SUCCESS);
		}
		return UserSummary.from(user);
	}

	/** Evita dejar el sistema sin un administrador activo que pueda gestionar cuentas. */
	private void ensureAnotherActiveAdminRemains(UserAccount target) {
		if (target.isActive() && target.hasRole(RoleCode.ADMIN)
				&& users.countByRole_CodeAndActiveTrue(RoleCode.ADMIN) <= 1) {
			throw new BusinessRuleException("Debe quedar al menos un administrador activo.");
		}
	}

	private UserAccount user(Long id) {
		return users.findWithRoleById(id)
				.orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta con id " + id + "."));
	}
}
