package co.sierraalta.production.auth.infrastructure.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import co.sierraalta.production.auth.domain.UserAccount;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Revalida en cada solicitud la cuenta guardada en la sesión.
 *
 * <ul>
 * <li>Si la cuenta fue desactivada, invalida la sesión: la solicitud continúa como anónima y recibe 401.</li>
 * <li>Si el administrador cambió el rol, actualiza las autoridades de inmediato, sin esperar un nuevo login.</li>
 * </ul>
 * Cuesta una consulta por clave primaria por solicitud autenticada, aceptable para el volumen previsto.
 */
public class AccountStatusFilter extends OncePerRequestFilter {

	private final UserAccountRepository users;
	private final SecurityContextRepository contextRepository;
	private final SecurityContextHolderStrategy holderStrategy = SecurityContextHolder.getContextHolderStrategy();

	public AccountStatusFilter(UserAccountRepository users, SecurityContextRepository contextRepository) {
		this.users = users;
		this.contextRepository = contextRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = holderStrategy.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
			Optional<UserAccount> current = users.findWithRoleById(principal.id());
			if (current.isEmpty() || !current.get().isActive()) {
				endSession(request);
			}
			else if (current.get().getRole().getCode() != principal.role()) {
				refreshAuthorities(authentication, AccountPrincipal.from(current.get()), request, response);
			}
		}
		chain.doFilter(request, response);
	}

	private void endSession(HttpServletRequest request) {
		holderStrategy.clearContext();
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}

	private void refreshAuthorities(Authentication previous, AccountPrincipal updated, HttpServletRequest request,
			HttpServletResponse response) {
		updated.eraseCredentials();
		Authentication refreshed = UsernamePasswordAuthenticationToken.authenticated(updated, null,
				mergeAuthorities(previous, updated));
		SecurityContext context = holderStrategy.createEmptyContext();
		context.setAuthentication(refreshed);
		holderStrategy.setContext(context);
		contextRepository.saveContext(context, request, response);
	}

	/**
	 * Sustituye las autoridades derivadas del rol y conserva las que Spring Security agregó al autenticar
	 * (por ejemplo, el factor de autenticación usado).
	 */
	private static Collection<GrantedAuthority> mergeAuthorities(Authentication previous, AccountPrincipal updated) {
		List<String> roleDerived = ((AccountPrincipal) previous.getPrincipal()).getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
		List<GrantedAuthority> merged = new ArrayList<>(updated.getAuthorities());
		previous.getAuthorities().stream()
				.filter(authority -> !roleDerived.contains(authority.getAuthority()))
				.forEach(merged::add);
		return merged;
	}
}
