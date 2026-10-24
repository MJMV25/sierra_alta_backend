package co.sierraalta.production.auth.infrastructure.persistence;

import java.util.Optional;

import co.sierraalta.production.auth.domain.Role;
import co.sierraalta.production.auth.domain.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByCode(RoleCode code);

	/** El catálogo de roles lo crea la migración V1; si falta uno, la base no corresponde al código. */
	default Role getByCode(RoleCode code) {
		return findByCode(code)
				.orElseThrow(() -> new IllegalStateException("El rol " + code + " no existe en la base de datos."));
	}
}
