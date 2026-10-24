package co.sierraalta.production.auth.infrastructure.bootstrap;

import java.util.Map;

import co.sierraalta.production.auth.domain.RoleCode;
import co.sierraalta.production.auth.domain.UserAccount;
import co.sierraalta.production.auth.domain.Usernames;
import co.sierraalta.production.auth.infrastructure.persistence.RoleRepository;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea las cuentas iniciales al arrancar. No hay autorregistro: el primer administrador sale de
 * configuración y luego administra las demás cuentas desde la API. Es idempotente.
 */
@Component
public class InitialAccountsInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(InitialAccountsInitializer.class);

	/** Nombre de usuario de demostración para cada rol operativo. */
	static final Map<String, RoleCode> DEMO_USERS = Map.of(
			"planeador", RoleCode.PLANNER,
			"supervisor", RoleCode.SUPERVISOR,
			"analista", RoleCode.ANALYST);

	private final BootstrapProperties properties;
	private final UserAccountRepository users;
	private final RoleRepository roles;
	private final PasswordEncoder passwordEncoder;

	public InitialAccountsInitializer(BootstrapProperties properties, UserAccountRepository users,
			RoleRepository roles, PasswordEncoder passwordEncoder) {
		this.properties = properties;
		this.users = users;
		this.roles = roles;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		createAdminIfMissing();
		if (properties.demoUsers().enabled()) {
			createDemoUsers();
		}
	}

	private void createAdminIfMissing() {
		if (users.countByRole_CodeAndActiveTrue(RoleCode.ADMIN) > 0) {
			return;
		}
		BootstrapProperties.Admin admin = properties.admin();
		if (!admin.configured()) {
			log.warn("No hay administradores activos. Defina BOOTSTRAP_ADMIN_USERNAME y BOOTSTRAP_ADMIN_PASSWORD "
					+ "para crear el primero.");
			return;
		}
		if (createIfAbsent(admin.username(), admin.password(), RoleCode.ADMIN)) {
			log.info("Administrador inicial '{}' creado.", Usernames.normalize(admin.username()));
		}
		else {
			log.warn("La cuenta '{}' ya existe pero no es un administrador activo; no se modifica.",
					Usernames.normalize(admin.username()));
		}
	}

	private void createDemoUsers() {
		String password = properties.demoUsers().password();
		if (password == null || password.isBlank()) {
			log.warn("Cuentas de demostración habilitadas sin DEMO_USERS_PASSWORD; no se crean.");
			return;
		}
		DEMO_USERS.forEach((username, role) -> {
			if (createIfAbsent(username, password, role)) {
				log.info("Cuenta de demostración '{}' ({}) creada.", username, role);
			}
		});
	}

	private boolean createIfAbsent(String username, String rawPassword, RoleCode roleCode) {
		String normalized = Usernames.normalize(username);
		if (users.existsByUsername(normalized)) {
			return false;
		}
		users.save(UserAccount.create(normalized, passwordEncoder.encode(rawPassword), roles.getByCode(roleCode)));
		return true;
	}
}
