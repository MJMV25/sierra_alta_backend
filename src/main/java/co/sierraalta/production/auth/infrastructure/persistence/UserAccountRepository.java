package co.sierraalta.production.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import co.sierraalta.production.auth.domain.RoleCode;
import co.sierraalta.production.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	/** Carga cuenta y rol en una sola consulta; se usa en cada autenticación. */
	@EntityGraph(attributePaths = "role")
	Optional<UserAccount> findByUsername(String username);

	/** Usado para revalidar la sesión en cada solicitud (estado y rol vigentes). */
	@EntityGraph(attributePaths = "role")
	Optional<UserAccount> findWithRoleById(Long id);

	@EntityGraph(attributePaths = "role")
	List<UserAccount> findAllByOrderByUsernameAsc();

	boolean existsByUsername(String username);

	long countByRole_CodeAndActiveTrue(RoleCode code);
}
