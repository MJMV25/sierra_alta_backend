package co.sierraalta.production.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Inicio de sesión con usuario y contraseña recibidos como JSON.
 *
 * <p>Verifica credenciales y estado de la cuenta, cambia el identificador de sesión (fijación de
 * sesión), renueva el token CSRF y guarda el contexto de seguridad en la sesión HTTP.
 */
@Component
public class SessionAuthenticator {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionStrategy;
	private final SecurityContextRepository contextRepository;
	private final SecurityContextHolderStrategy holderStrategy = SecurityContextHolder.getContextHolderStrategy();

	public SessionAuthenticator(AuthenticationManager authenticationManager,
			SessionAuthenticationStrategy sessionStrategy, SecurityContextRepository contextRepository) {
		this.authenticationManager = authenticationManager;
		this.sessionStrategy = sessionStrategy;
		this.contextRepository = contextRepository;
	}

	/**
	 * @throws org.springframework.security.core.AuthenticationException si las credenciales son
	 * incorrectas o la cuenta está inactiva
	 */
	public AccountPrincipal login(String username, String password, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication = authenticationManager
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));

		sessionStrategy.onAuthentication(authentication, request, response);

		SecurityContext context = holderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		holderStrategy.setContext(context);
		contextRepository.saveContext(context, request, response);

		return (AccountPrincipal) authentication.getPrincipal();
	}
}
