package co.sierraalta.production.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import co.sierraalta.production.ApiIntegrationTest;
import jakarta.servlet.http.Cookie;
import co.sierraalta.production.auth.domain.SecurityEvent;
import co.sierraalta.production.auth.domain.SecurityEventResult;
import co.sierraalta.production.auth.domain.SecurityEventType;
import co.sierraalta.production.auth.infrastructure.persistence.SecurityEventRepository;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/** Escenario de seguridad: inicio de sesión, rechazo de credenciales, cuentas inactivas y cierre de sesión. */
class AuthenticationFlowTest extends ApiIntegrationTest {

	@Autowired
	UserAccountRepository users;

	@Autowired
	SecurityEventRepository events;

	@Test
	void csrfEndpointIssuesReadableCookieAndHeaderName() throws Exception {
		mvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
				.andExpect(jsonPath("$.token", not(emptyString())))
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andExpect(cookie().httpOnly("XSRF-TOKEN", false));
	}

	@Test
	void loginWithoutCsrfTokenIsRejected() throws Exception {
		mvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials(ADMIN, ADMIN_PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("csrf-invalid"));
	}

	@Test
	void csrfRejectionIsAuditedApartFromRoleDenials() throws Exception {
		long before = events.count();

		mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(ADMIN, ADMIN_PASSWORD)));

		List<SecurityEvent> recent = events.findAll(Sort.by("id")).stream().skip(before).toList();
		assertThat(recent).extracting(SecurityEvent::getType)
				.contains(SecurityEventType.CSRF_REJECTED)
				.doesNotContain(SecurityEventType.ACCESS_DENIED);
	}

	@Test
	void malformedJsonGetsAReadableProblem() throws Exception {
		mvc.perform(post("/api/v1/auth/login").with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("malformed-request"))
				.andExpect(jsonPath("$.detail")
						.value("El cuerpo de la solicitud no es un JSON válido o no tiene el formato esperado."));
	}

	@Test
	void oversizedUsernameIsReportedInSpanish() throws Exception {
		mvc.perform(post("/api/v1/auth/login").with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials("u".repeat(51), "cualquier-clave")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation"))
				.andExpect(jsonPath("$.errors[0].message").value("El nombre de usuario no puede superar 50 caracteres."));
	}

	@Test
	void successfulLoginReturnsRoleAndScope() throws Exception {
		mvc.perform(post("/api/v1/auth/login").with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials("planeador", DEMO_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("planeador"))
				.andExpect(jsonPath("$.role").value("PLANNER"))
				.andExpect(jsonPath("$.roleName").value("Planeador de producción"))
				.andExpect(jsonPath("$.permissions", containsInAnyOrder(
						"history:read", "forecast:execute", "forecast:read", "indicators:read")))
				.andExpect(jsonPath("$.password").doesNotExist())
				// El login renueva el token CSRF.
				.andExpect(cookie().exists("XSRF-TOKEN"));
	}

	@Test
	void csrfHeaderThatDoesNotMatchTheCookieIsRejected() throws Exception {
		mvc.perform(post("/api/v1/auth/login")
					.cookie(new Cookie("XSRF-TOKEN", "token-legitimo"))
					.header("X-XSRF-TOKEN", "token-falsificado")
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials(ADMIN, ADMIN_PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("csrf-invalid"));
	}

	@Test
	void usernameIsCaseInsensitive() throws Exception {
		login("  SuperVisor ", DEMO_PASSWORD);
	}

	@Test
	void wrongPasswordIsRejectedWithGenericMessage() throws Exception {
		mvc.perform(post("/api/v1/auth/login").with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials(ADMIN, "incorrecta-123")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid-credentials"))
				.andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos."));
	}

	@Test
	void unknownUserGetsTheSameResponseAsWrongPassword() throws Exception {
		mvc.perform(post("/api/v1/auth/login").with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials("no-existe", "cualquier-clave")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid-credentials"))
				.andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos."));
	}

	@Test
	void inactiveAccountCannotLogIn() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		String username = uniqueUsername("inactivo");
		long id = createUser(admin, username, "clave-segura-123", "ANALYST");
		mvc.perform(patch("/api/v1/admin/users/{id}/status", id).session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"active\": false}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/v1/auth/login").with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials(username, "clave-segura-123")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos."));
	}

	@Test
	void blankCredentialsAreReportedPerField() throws Exception {
		mvc.perform(post("/api/v1/auth/login").with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials("", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation"))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("username", "password")));
	}

	@Test
	void sessionGivesAccessToCurrentUserUntilLogout() throws Exception {
		MockHttpSession session = login("analista", DEMO_PASSWORD);

		mvc.perform(get("/api/v1/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("analista"))
				.andExpect(jsonPath("$.role").value("ANALYST"));

		mvc.perform(post("/api/v1/auth/logout").session(session).with(xsrf()))
				.andExpect(status().isNoContent());

		assertThat(session.isInvalid()).isTrue();
		mvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("authentication-required"));
	}

	@Test
	void loginChangesTheSessionIdentifier() throws Exception {
		MockHttpSession anonymous = new MockHttpSession();
		String before = anonymous.getId();
		MockHttpSession after = (MockHttpSession) mvc.perform(post("/api/v1/auth/login").session(anonymous).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(credentials("supervisor", DEMO_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
		assertThat(after.getId()).isNotEqualTo(before);
	}

	@Test
	void loginAttemptsAreAudited() throws Exception {
		long adminId = users.findByUsername(ADMIN).orElseThrow().getId();
		long before = events.count();

		mvc.perform(post("/api/v1/auth/login").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(ADMIN, "incorrecta-123")));
		login(ADMIN, ADMIN_PASSWORD);

		List<SecurityEvent> recent = events.findAll(Sort.by("id")).stream().skip(before).toList();
		assertThat(recent).anySatisfy(event -> {
			assertThat(event.getType()).isEqualTo(SecurityEventType.LOGIN);
			assertThat(event.getResult()).isEqualTo(SecurityEventResult.FAILURE);
			assertThat(event.getUserId()).isEqualTo(adminId);
		});
		assertThat(recent).anySatisfy(event -> {
			assertThat(event.getType()).isEqualTo(SecurityEventType.LOGIN);
			assertThat(event.getResult()).isEqualTo(SecurityEventResult.SUCCESS);
			assertThat(event.getUserId()).isEqualTo(adminId);
		});
	}

	@Test
	void passwordsAreStoredAsAdaptiveHash() {
		String hash = users.findByUsername(ADMIN).orElseThrow().getPasswordHash();
		assertThat(hash).startsWith("{bcrypt}").doesNotContain(ADMIN_PASSWORD);
	}
}
