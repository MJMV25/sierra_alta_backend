package co.sierraalta.production;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base de las pruebas de integración: contexto completo, cadena de seguridad real y H2 con las
 * migraciones Flyway. Las cuentas iniciales vienen de {@code application-test.yml}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

	protected static final String ADMIN = "admin";
	protected static final String ADMIN_PASSWORD = "admin-password-123";
	protected static final String DEMO_PASSWORD = "demo-password-123";

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@Autowired
	protected MockMvc mvc;

	/** Inicia sesión por el endpoint real y devuelve la sesión HTTP resultante. */
	protected MockHttpSession login(String username, String password) throws Exception {
		return (MockHttpSession) mvc.perform(post("/api/v1/auth/login").with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials(username, password)))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
	}

	/** Crea una cuenta con la API de administración y devuelve su id. */
	protected long createUser(MockHttpSession adminSession, String username, String password, String role)
			throws Exception {
		String body = mvc.perform(post("/api/v1/admin/users").session(adminSession).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username": "%s", "password": "%s", "role": "%s"}""".formatted(username, password, role)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return Long.parseLong(body.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
	}

	protected static String credentials(String username, String password) {
		return """
				{"username": "%s", "password": "%s"}""".formatted(username, password);
	}

	/**
	 * CSRF real por doble envío, como lo hará React: cookie {@code XSRF-TOKEN} y cabecera
	 * {@code X-XSRF-TOKEN} con el mismo valor. No se usa {@code csrf()} de spring-security-test porque
	 * sustituye el repositorio de tokens del filtro y ocultaría la configuración real.
	 */
	protected static RequestPostProcessor xsrf() {
		return request -> {
			String token = UUID.randomUUID().toString();
			request.setCookies(new Cookie("XSRF-TOKEN", token));
			request.addHeader("X-XSRF-TOKEN", token);
			return request;
		};
	}

	/** Nombre de usuario único para que cada prueba trabaje con sus propias cuentas. */
	protected static String uniqueUsername(String prefix) {
		return prefix + "-" + SEQUENCE.incrementAndGet();
	}
}
