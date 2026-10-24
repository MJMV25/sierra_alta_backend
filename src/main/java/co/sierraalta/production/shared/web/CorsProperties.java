package co.sierraalta.production.shared.web;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Orígenes del frontend React autorizados a llamar la API con credenciales (cookie de sesión).
 *
 * @param allowedOrigins orígenes exactos, por ejemplo {@code http://localhost:5173}; no se admite {@code *}.
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
	}
}
