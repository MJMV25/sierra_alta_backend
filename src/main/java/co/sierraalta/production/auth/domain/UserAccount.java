package co.sierraalta.production.auth.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Cuenta administrada del sistema (no hay autorregistro). Un usuario tiene exactamente un rol.
 * Las cuentas no se eliminan: se desactivan para conservar la autoría de ejecuciones.
 */
@Entity
@Table(name = "usuario")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rol_id", nullable = false)
	private Role role;

	@Column(name = "nombre_usuario", nullable = false, unique = true, updatable = false, length = 50)
	private String username;

	/** Hash adaptativo con prefijo de algoritmo, por ejemplo {@code {bcrypt}...}. Nunca texto plano. */
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "activo", nullable = false)
	private boolean active;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant createdAt;

	protected UserAccount() {
		// Requerido por JPA.
	}

	private UserAccount(String username, String passwordHash, Role role) {
		this.username = Usernames.normalize(username);
		this.passwordHash = Objects.requireNonNull(passwordHash);
		this.role = Objects.requireNonNull(role);
		this.active = true;
		this.createdAt = Instant.now();
	}

	/** Crea una cuenta activa. El hash debe venir ya calculado por el codificador de contraseñas. */
	public static UserAccount create(String username, String passwordHash, Role role) {
		return new UserAccount(username, passwordHash, role);
	}

	public void assignRole(Role newRole) {
		this.role = Objects.requireNonNull(newRole);
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}

	public boolean hasRole(RoleCode code) {
		return role.getCode() == code;
	}

	public Long getId() {
		return id;
	}

	public Role getRole() {
		return role;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
