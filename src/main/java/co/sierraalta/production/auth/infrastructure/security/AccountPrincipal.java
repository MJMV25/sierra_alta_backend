package co.sierraalta.production.auth.infrastructure.security;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import co.sierraalta.production.auth.domain.Permission;
import co.sierraalta.production.auth.domain.RoleCode;
import co.sierraalta.production.auth.domain.UserAccount;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Identidad autenticada que se guarda en la sesión HTTP.
 *
 * <p>Sus autoridades son {@code ROLE_<rol>} más un permiso por cada operación que el rol concede
 * (por ejemplo {@code forecast:execute}). El hash se borra tras autenticar.
 */
public final class AccountPrincipal implements UserDetails, CredentialsContainer {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Long id;
	private final String username;
	private final RoleCode role;
	private final String roleName;
	private final boolean active;
	private String passwordHash;

	private AccountPrincipal(Long id, String username, RoleCode role, String roleName, boolean active,
			String passwordHash) {
		this.id = id;
		this.username = username;
		this.role = role;
		this.roleName = roleName;
		this.active = active;
		this.passwordHash = passwordHash;
	}

	public static AccountPrincipal from(UserAccount user) {
		return new AccountPrincipal(user.getId(), user.getUsername(), user.getRole().getCode(),
				user.getRole().getName(), user.isActive(), user.getPasswordHash());
	}

	public Long id() {
		return id;
	}

	public RoleCode role() {
		return role;
	}

	public String roleName() {
		return roleName;
	}

	public Set<Permission> permissions() {
		return role.permissions();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
		role.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.authority())));
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	/** Una cuenta inactiva provoca {@code DisabledException} y no puede iniciar sesión. */
	@Override
	public boolean isEnabled() {
		return active;
	}

	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AccountPrincipal that && id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "AccountPrincipal[id=" + id + ", username=" + username + ", role=" + role + "]";
	}
}
