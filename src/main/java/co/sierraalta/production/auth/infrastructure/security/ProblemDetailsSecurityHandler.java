package co.sierraalta.production.auth.infrastructure.security;

import co.sierraalta.production.auth.application.SecurityEventRecorder;
import co.sierraalta.production.auth.domain.SecurityEventResult;
import co.sierraalta.production.auth.domain.SecurityEventType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Respuestas 401/403 emitidas por los filtros de seguridad. Delegan en el
 * {@code GlobalExceptionHandler} para usar el mismo formato {@code problem+json} que la API.
 * Una API REST no redirige a una página de login.
 */
@Component
public class ProblemDetailsSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;
	private final SecurityEventRecorder eventRecorder;

	public ProblemDetailsSecurityHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
			SecurityEventRecorder eventRecorder) {
		this.resolver = resolver;
		this.eventRecorder = eventRecorder;
	}

	/** Sin sesión válida: 401. */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		resolver.resolveException(request, response, null, authException);
	}

	/**
	 * 403 auditado. Se distingue el token CSRF ausente o inválido ({@code CSRF_REJECTED}) de la falta
	 * de permiso del rol ({@code ACCESS_DENIED}), porque el usuario debe corregir cosas distintas.
	 */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long userId = authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
				? principal.id() : null;
		SecurityEventType type = accessDeniedException instanceof CsrfException
				? SecurityEventType.CSRF_REJECTED : SecurityEventType.ACCESS_DENIED;
		eventRecorder.record(userId, type, SecurityEventResult.DENIED);
		resolver.resolveException(request, response, null, accessDeniedException);
	}
}
