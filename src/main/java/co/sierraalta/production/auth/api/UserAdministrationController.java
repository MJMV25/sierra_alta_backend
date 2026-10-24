package co.sierraalta.production.auth.api;

import java.net.URI;
import java.util.List;

import co.sierraalta.production.auth.api.dto.ChangeRoleRequest;
import co.sierraalta.production.auth.api.dto.ChangeStatusRequest;
import co.sierraalta.production.auth.api.dto.CreateUserRequest;
import co.sierraalta.production.auth.application.RoleSummary;
import co.sierraalta.production.auth.application.SecurityEventSummary;
import co.sierraalta.production.auth.application.UserAdministrationService;
import co.sierraalta.production.auth.application.UserSummary;
import co.sierraalta.production.auth.infrastructure.security.AccountPrincipal;
import co.sierraalta.production.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administración de cuentas. Requiere el permiso {@code users:manage} (rol ADMIN). */
@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
public class UserAdministrationController {

	private final UserAdministrationService administration;

	public UserAdministrationController(UserAdministrationService administration) {
		this.administration = administration;
	}

	@GetMapping("/roles")
	public List<RoleSummary> roles() {
		return administration.listRoles();
	}

	@GetMapping("/users")
	public List<UserSummary> users() {
		return administration.listUsers();
	}

	@PostMapping("/users")
	public ResponseEntity<UserSummary> create(@Valid @RequestBody CreateUserRequest body,
			@AuthenticationPrincipal AccountPrincipal actor) {
		UserSummary created = administration.createUser(body.username(), body.password(), body.role(), actor.id());
		return ResponseEntity.created(URI.create(ApiPaths.V1 + "/admin/users/" + created.id())).body(created);
	}

	@PatchMapping("/users/{id}/role")
	public UserSummary changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest body,
			@AuthenticationPrincipal AccountPrincipal actor) {
		return administration.changeRole(id, body.role(), actor.id());
	}

	@PatchMapping("/users/{id}/status")
	public UserSummary changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest body,
			@AuthenticationPrincipal AccountPrincipal actor) {
		return administration.changeStatus(id, body.active(), actor.id());
	}

	@GetMapping("/security-events")
	public List<SecurityEventSummary> securityEvents(@RequestParam(defaultValue = "100") int limit) {
		return administration.recentEvents(limit);
	}
}
