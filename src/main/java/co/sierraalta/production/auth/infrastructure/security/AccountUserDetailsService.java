package co.sierraalta.production.auth.infrastructure.security;

import co.sierraalta.production.auth.domain.Usernames;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lee la cuenta y su rol desde PostgreSQL para que Spring Security verifique la contraseña. */
@Service
public class AccountUserDetailsService implements UserDetailsService {

	private final UserAccountRepository users;

	public AccountUserDetailsService(UserAccountRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public AccountPrincipal loadUserByUsername(String username) {
		return users.findByUsername(Usernames.normalize(username))
				.map(AccountPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("Cuenta inexistente"));
	}
}
