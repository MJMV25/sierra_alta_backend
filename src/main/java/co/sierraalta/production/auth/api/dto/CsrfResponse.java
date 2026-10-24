package co.sierraalta.production.auth.api.dto;

/** Nombre de la cabecera y valor del token CSRF que debe enviarse en solicitudes que modifican estado. */
public record CsrfResponse(String headerName, String token) {
}
