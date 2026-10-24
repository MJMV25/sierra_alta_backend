package co.sierraalta.production.auth.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cuentas iniciales. Las contraseñas llegan por variables de entorno y nunca se versionan.
 *
 * @param admin primer administrador, creado solo si aún no existe ningún ADMIN
 * @param demoUsers cuentas de demostración, una por rol operativo (solo para desarrollo)
 */
@ConfigurationProperties("app.bootstrap")
public record BootstrapProperties(Admin admin, DemoUsers demoUsers) {

	public BootstrapProperties {
		admin = admin == null ? new Admin(null, null) : admin;
		demoUsers = demoUsers == null ? new DemoUsers(false, null) : demoUsers;
	}

	public record Admin(String username, String password) {

		boolean configured() {
			return username != null && !username.isBlank() && password != null && !password.isBlank();
		}
	}

	public record DemoUsers(boolean enabled, String password) {
	}
}
