package co.sierraalta.production.shared.web;

import java.net.URI;
import java.util.List;

import co.sierraalta.production.shared.error.BusinessRuleException;
import co.sierraalta.production.shared.error.ConflictException;
import co.sierraalta.production.shared.error.FeatureNotImplementedException;
import co.sierraalta.production.shared.error.InvalidFieldException;
import co.sierraalta.production.shared.error.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce excepciones a respuestas RFC 9457 ({@code application/problem+json}) con un formato único.
 *
 * <p>Los errores de seguridad producidos en los filtros también llegan aquí (ver
 * {@code ProblemDetailsSecurityHandler}), de modo que el frontend recibe siempre la misma estructura.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler({BadCredentialsException.class, AccountStatusException.class})
	ProblemDetail handleInvalidCredentials() {
		// Mismo mensaje para usuario inexistente, contraseña errónea o cuenta inactiva: no revela cuál falló.
		return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Usuario o contraseña incorrectos.");
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthenticationRequired() {
		return problem(HttpStatus.UNAUTHORIZED, "authentication-required", "Debe iniciar sesión para continuar.");
	}

	/** Token CSRF ausente, vencido o que no coincide con la cookie. No es un problema de permisos del rol. */
	@ExceptionHandler(CsrfException.class)
	ProblemDetail handleCsrf() {
		return problem(HttpStatus.FORBIDDEN, "csrf-invalid",
				"No se pudo verificar la solicitud. Recargue la página e intente de nuevo.");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "access-denied", "Su rol no tiene permiso para realizar esta operación.");
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "not-found", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "conflict", ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
		return problem(HttpStatus.CONFLICT, "conflict", "La operación viola una restricción de integridad de los datos.");
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "business-rule", ex.getMessage());
	}

	@ExceptionHandler(InvalidFieldException.class)
	ProblemDetail handleInvalidField(InvalidFieldException ex) {
		return validationProblem(List.of(new FieldError(ex.field(), ex.getMessage())));
	}

	@ExceptionHandler(FeatureNotImplementedException.class)
	ProblemDetail handleNotImplemented(FeatureNotImplementedException ex) {
		return problem(HttpStatus.NOT_IMPLEMENTED, "not-implemented", ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Error no controlado", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Ocurrió un error inesperado.");
	}

	/** Errores de validación de Bean Validation: se devuelven por campo para mostrarlos junto al formulario. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest().body(validationProblem(errors));
	}

	private static ProblemDetail validationProblem(List<FieldError> errors) {
		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "validation", "La solicitud contiene datos inválidos.");
		body.setProperty("errors", errors);
		return body;
	}

	/**
	 * Errores de petición que resuelve Spring MVC (JSON malformado, parámetro con tipo incorrecto, ruta
	 * o método inexistente, tipo de contenido no admitido): mismo formato y mensajes en español, sin
	 * exponer el detalle técnico del framework.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ProblemDetail problem = switch (ex) {
			case HttpMessageNotReadableException e -> problem(HttpStatus.BAD_REQUEST, "malformed-request",
					"El cuerpo de la solicitud no es un JSON válido o no tiene el formato esperado.");
			case TypeMismatchException e -> problem(HttpStatus.BAD_REQUEST, "invalid-parameter",
					e.getPropertyName() == null ? "Un parámetro de la solicitud no tiene un valor válido."
							: "El parámetro '" + e.getPropertyName() + "' no tiene un valor válido.");
			case MissingServletRequestParameterException e -> problem(HttpStatus.BAD_REQUEST, "invalid-parameter",
					"Falta el parámetro obligatorio '" + e.getParameterName() + "'.");
			case NoResourceFoundException e -> problem(HttpStatus.NOT_FOUND, "not-found",
					"El recurso solicitado no existe.");
			case HttpRequestMethodNotSupportedException e -> problem(HttpStatus.METHOD_NOT_ALLOWED,
					"method-not-allowed", "La operación no está disponible para este recurso.");
			case HttpMediaTypeNotSupportedException e -> problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
					"unsupported-media-type", "Envíe el cuerpo de la solicitud como application/json.");
			default -> problem(HttpStatus.valueOf(statusCode.value()), "request-error",
					"No se pudo procesar la solicitud.");
		};
		return super.handleExceptionInternal(ex, problem, headers, statusCode, request);
	}

	private static ProblemDetail problem(HttpStatus status, String code, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("urn:sierra-alta:problem:" + code));
		problem.setProperty("code", code);
		return problem;
	}

	record FieldError(String field, String message) {
	}
}
