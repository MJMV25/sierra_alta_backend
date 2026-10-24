package co.sierraalta.production.auth.domain;

import java.util.Locale;

/** Regla única de normalización de nombres de usuario: sin espacios externos y en minúsculas. */
public final class Usernames {

	/** Formato admitido al crear cuentas. */
	public static final String PATTERN = "^[a-zA-Z0-9._-]{3,50}$";

	private Usernames() {
	}

	public static String normalize(String username) {
		return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
	}
}
