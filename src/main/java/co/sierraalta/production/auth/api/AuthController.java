package co.sierraalta.production.auth.api;

import co.sierraalta.production.auth.api.dto.CsrfResponse;
import co.sierraalta.production.auth.api.dto.LoginRequest;
import co.sierraalta.production.auth.api.dto.SessionResponse;
import co.sierraalta.production.auth.infrastructure.security.AccountPrincipal;
import co.sierraalta.production.auth.infrastructure.security.SessionAuthenticator;
import co.sierraalta.production.shared.web.ApiPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicio y consulta de sesión. El cierre ({@code POST /api/v1/auth/logout}) lo atiende el filtro de
 * Spring Security configurado en {@code SecurityConfig}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
public class AuthController {

	private final SessionAuthenticator sessionAuthenticator;

	public AuthController(SessionAuthenticator sessionAuthenticator) {
		this.sessionAuthenticator = sessionAuthenticator;
	}

	/** Paso previo al login: emite la cookie {@code XSRF-TOKEN} y devuelve el mismo valor. */
	@GetMapping("/csrf")
	public CsrfResponse csrf(CsrfToken csrfToken) {
		return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
	}

	@PostMapping("/login")
	public SessionResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		AccountPrincipal principal = sessionAuthenticator.login(body.username(), body.password(), request, response);
		issueRotatedCsrfToken(request);
		return SessionResponse.from(principal);
	}

	@GetMapping("/me")
	public SessionResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
		return SessionResponse.from(principal);
	}

	/** El login renueva el token CSRF; leerlo fuerza el envío de la nueva cookie en esta respuesta. */
	private static void issueRotatedCsrfToken(HttpServletRequest request) {
		if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
			token.getToken();
		}
	}
}
