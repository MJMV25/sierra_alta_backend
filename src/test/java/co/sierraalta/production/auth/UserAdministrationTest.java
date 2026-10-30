package co.sierraalta.production.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sierraalta.production.ApiIntegrationTest;
import co.sierraalta.production.auth.application.UserAdministrationService;
import co.sierraalta.production.auth.infrastructure.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

/** Alcance del administrador: crear y desactivar cuentas y asignar rol, con efecto inmediato. */
class UserAdministrationTest extends ApiIntegrationTest {

	private static final String PASSWORD = "clave-segura-123";

	@Autowired
	UserAccountRepository users;

	@Autowired
	UserAdministrationService administration;

	@Test
	void adminCreatesAccountThatCanLogInWithItsRole() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		String username = uniqueUsername("Nuevo.Planeador");

		mvc.perform(post("/api/v1/admin/users").session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username": "%s", "password": "%s", "role": "PLANNER"}""".formatted(username, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.username").value(username.toLowerCase()))
				.andExpect(jsonPath("$.role").value("PLANNER"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		MockHttpSession session = login(username, PASSWORD);
		mvc.perform(get("/api/v1/auth/me").session(session))
				.andExpect(jsonPath("$.role").value("PLANNER"));
	}

	@Test
	void duplicateUsernameIsAConflict() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		String username = uniqueUsername("duplicado");
		createUser(admin, username, PASSWORD, "SUPERVISOR");

		mvc.perform(post("/api/v1/admin/users").session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username": "%s", "password": "%s", "role": "ANALYST"}""".formatted(username.toUpperCase(), PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("conflict"));
	}

	@Test
	void invalidAccountDataIsReportedPerField() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		mvc.perform(post("/api/v1/admin/users").session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username": "a b", "password": "corta", "role": null}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItems("username", "password", "role")));
	}

	@Test
	void deactivatingAnAccountEndsItsOpenSession() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		String username = uniqueUsername("desactivado");
		long id = createUser(admin, username, PASSWORD, "ANALYST");
		MockHttpSession userSession = login(username, PASSWORD);
		mvc.perform(get("/api/v1/auth/me").session(userSession)).andExpect(status().isOk());

		mvc.perform(patch("/api/v1/admin/users/{id}/status", id).session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"active\": false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		mvc.perform(get("/api/v1/auth/me").session(userSession))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void reactivatedAccountCanLogInAgain() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		String username = uniqueUsername("reactivado");
		long id = createUser(admin, username, PASSWORD, "SUPERVISOR");
		setActive(admin, id, false);
		setActive(admin, id, true);

		login(username, PASSWORD);
	}

	@Test
	void roleChangeAppliesToTheOpenSessionImmediately() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		String username = uniqueUsername("cambio-rol");
		long id = createUser(admin, username, PASSWORD, "SUPERVISOR");
		MockHttpSession userSession = login(username, PASSWORD);

		mvc.perform(post("/api/v1/forecasts").session(userSession).with(xsrf()))
				.andExpect(status().isForbidden());

		mvc.perform(patch("/api/v1/admin/users/{id}/role", id).session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"PLANNER\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("PLANNER"));

		// Ya no es 403: pasa la autorización y falla solo porque se envía sin cuerpo.
		mvc.perform(post("/api/v1/forecasts").session(userSession).with(xsrf()))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/auth/me").session(userSession))
				.andExpect(jsonPath("$.role").value("PLANNER"))
				.andExpect(jsonPath("$.permissions", hasItem("forecast:execute")));
	}

	@Test
	void adminCannotDeactivateOrDemoteThemselves() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		long adminId = users.findByUsername(ADMIN).orElseThrow().getId();

		mvc.perform(patch("/api/v1/admin/users/{id}/status", adminId).session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"active\": false}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("business-rule"));

		mvc.perform(patch("/api/v1/admin/users/{id}/role", adminId).session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"ANALYST\"}"))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void anotherAdminCanBeDeactivatedWhileOneRemains() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		long secondAdmin = createUser(admin, uniqueUsername("admin2"), PASSWORD, "ADMIN");

		setActive(admin, secondAdmin, false);
	}

	@Test
	void unknownAccountIsNotFound() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		mvc.perform(patch("/api/v1/admin/users/{id}/status", 999_999).session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"active\": false}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rolesCatalogExposesEachScope() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		mvc.perform(get("/api/v1/admin/roles").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.code == 'ADMIN')].permissions[*]", hasItem("users:manage")))
				.andExpect(jsonPath("$[?(@.code == 'ANALYST')].permissions[*]", hasItem("history:write")));
	}

	@Test
	void stateChangingRequestsRequireCsrfEvenWithASession() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		mvc.perform(post("/api/v1/admin/users").session(admin)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"username": "%s", "password": "%s", "role": "ANALYST"}""".formatted(uniqueUsername("sin-csrf"), PASSWORD)))
				.andExpect(status().isForbidden());
	}

	@Test
	void nonNumericIdIsAnInvalidParameter() throws Exception {
		MockHttpSession admin = login(ADMIN, ADMIN_PASSWORD);
		mvc.perform(patch("/api/v1/admin/users/{id}/status", "abc").session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"active\": false}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid-parameter"));
	}

	@Test
	@WithMockUser(authorities = "history:read")
	void useCaseRequiresPermissionEvenOutsideTheHttpRoute() {
		assertThatThrownBy(() -> administration.listUsers()).isInstanceOf(AccessDeniedException.class);
	}

	private void setActive(MockHttpSession admin, long id, boolean active) throws Exception {
		mvc.perform(patch("/api/v1/admin/users/{id}/status", id).session(admin).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("{\"active\": " + active + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(active));
	}
}
