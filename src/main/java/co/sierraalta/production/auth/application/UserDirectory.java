package co.sierraalta.production.auth.application;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import co.sierraalta.production.auth.domain.UserAccount;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio público de {@code auth} para otros módulos: solo traduce ids de usuario a su nombre de usuario,
 * para mostrar quién ejecutó un pronóstico (trazabilidad, respuesta 23). No expone roles, estado ni datos
 * de acceso, por eso no exige un permiso propio: quien la invoca ya validó el suyo.
 */
@Service
@Transactional(readOnly = true)
public class UserDirectory {

	private final UserAccountRepository users;

	public UserDirectory(UserAccountRepository users) {
		this.users = users;
	}

	/** Nombre de usuario por id; los ids que no existen no aparecen en el mapa. */
	public Map<Long, String> usernames(Collection<Long> ids) {
		return users.findAllById(ids).stream()
				.collect(Collectors.toMap(UserAccount::getId, UserAccount::getUsername));
	}
}
