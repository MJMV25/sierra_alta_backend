package co.sierraalta.production.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import co.sierraalta.production.ApiIntegrationTest;
import co.sierraalta.production.auth.domain.SecurityEventResult;
import co.sierraalta.production.auth.domain.SecurityEventType;
import co.sierraalta.production.auth.infrastructure.persistence.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;

/**
 * Prueba de acceso directo por rol: cada rol contra cada endpoint protegido.
 *
 * <p>Permitido: el endpoint responde (200 en administración, 501 en módulos aún no implementados).
 * Denegado: 403. Sin sesión: 401. La matriz replica la tabla 2 del documento de arquitectura.
 */
class RoleScopeTest extends ApiIntegrationTest {

	private static final Set<String> ADMIN_ONLY = Set.of("ADMIN");
	private static final Set<String> READERS = Set.of("PLANNER", "SUPERVISOR", "ANALYST");
	private static final Set<String> EXECUTORS = Set.of("PLANNER", "ANALYST");
	private static final Set<String> LOADERS = Set.of("ANALYST");

	private static final Map<String, String> USER_BY_ROLE = Map.of(
			"ADMIN", ADMIN,
			"PLANNER", "planeador",
			"SUPERVISOR", "supervisor",
			"ANALYST", "analista");

	record Endpoint(HttpMethod method, String path, Set<String> allowedRoles, int allowedStatus) {

		@Override
		public String toString() {
			return method + " " + path;
		}
	}

	static final List<Endpoint> ENDPOINTS = List.of(
			new Endpoint(HttpMethod.GET, "/api/v1/admin/users", ADMIN_ONLY, 200),
			new Endpoint(HttpMethod.GET, "/api/v1/admin/roles", ADMIN_ONLY, 200),
			new Endpoint(HttpMethod.GET, "/api/v1/admin/security-events", ADMIN_ONLY, 200),
			new Endpoint(HttpMethod.GET, "/api/v1/history/products", READERS, 501),
			new Endpoint(HttpMethod.GET, "/api/v1/history/series/1/observations", READERS, 501),
			new Endpoint(HttpMethod.POST, "/api/v1/history/series/1/observations", LOADERS, 501),
			new Endpoint(HttpMethod.POST, "/api/v1/forecasts", EXECUTORS, 501),
			new Endpoint(HttpMethod.GET, "/api/v1/forecasts", READERS, 501),
			new Endpoint(HttpMethod.GET, "/api/v1/forecasts/1", READERS, 501),
			new Endpoint(HttpMethod.GET, "/api/v1/indicators/executions/1", READERS, 501));

	static Stream<Arguments> roleEndpointMatrix() {
		return USER_BY_ROLE.keySet().stream().sorted()
				.flatMap(role -> ENDPOINTS.stream().map(endpoint -> Arguments.of(role, endpoint)));
	}

	static Stream<Endpoint> endpoints() {
		return ENDPOINTS.stream();
	}

	@Autowired
	SecurityEventRepository events;

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("roleEndpointMatrix")
	void eachRoleReachesOnlyItsScope(String role, Endpoint endpoint) throws Exception {
		MockHttpSession session = login(USER_BY_ROLE.get(role), "ADMIN".equals(role) ? ADMIN_PASSWORD : DEMO_PASSWORD);
		boolean allowed = endpoint.allowedRoles().contains(role);

		mvc.perform(request(endpoint.method(), endpoint.path()).session(session).with(xsrf()))
				.andExpect(status().is(allowed ? endpoint.allowedStatus() : 403));
	}

	@ParameterizedTest(name = "anónimo -> {0}")
	@MethodSource("endpoints")
	void anonymousRequestsNeedASession(Endpoint endpoint) throws Exception {
		mvc.perform(request(endpoint.method(), endpoint.path()).with(xsrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("authentication-required"));
	}

	@Test
	void undeclaredEndpointsAreDeniedByDefault() throws Exception {
		MockHttpSession session = login(ADMIN, ADMIN_PASSWORD);
		mvc.perform(get("/api/v1/no-declarado").session(session))
				.andExpect(status().isForbidden());
		mvc.perform(request(HttpMethod.DELETE, "/api/v1/forecasts/1").session(session).with(xsrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void deniedAccessReturnsProblemDetailAndIsAudited() throws Exception {
		MockHttpSession session = login("supervisor", DEMO_PASSWORD);
		long before = events.count();

		mvc.perform(request(HttpMethod.POST, "/api/v1/forecasts").session(session).with(xsrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("access-denied"))
				.andExpect(jsonPath("$.status").value(403));

		assertThat(events.findAll()).filteredOn(event -> event.getType() == SecurityEventType.ACCESS_DENIED
				&& event.getResult() == SecurityEventResult.DENIED).hasSizeGreaterThan(0);
		assertThat(events.count()).isGreaterThan(before);
	}

	@Test
	void matrixCoversEveryRole() {
		assertThat(USER_BY_ROLE.keySet()).containsExactlyInAnyOrderElementsOf(
				Arrays.stream(co.sierraalta.production.auth.domain.RoleCode.values()).map(Enum::name).toList());
	}
}
