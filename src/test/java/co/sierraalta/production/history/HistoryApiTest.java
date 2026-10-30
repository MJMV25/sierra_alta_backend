package co.sierraalta.production.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.ApiIntegrationTest;
import co.sierraalta.production.history.application.HistoryService;
import co.sierraalta.production.history.application.NewObservation;
import co.sierraalta.production.history.domain.DataLoad;
import co.sierraalta.production.history.infrastructure.persistence.DataLoadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Carga y consulta de históricos por la API real. Cada prueba usa su propia serie para no depender del
 * orden de ejecución; las 4 series del catálogo quedan vacías.
 */
class HistoryApiTest extends ApiIntegrationTest {

	@Autowired
	DataLoadRepository loads;

	@Autowired
	HistoryService history;

	/** Sesión del analista, abierta al primer uso: un login previo borraría el contexto de {@code @WithMockUser}. */
	private MockHttpSession analystSession;

	@Test
	void catalogListsTheFourReferencesAsMonthlyProduction() throws Exception {
		mvc.perform(get("/api/v1/history/products").session(login("supervisor", DEMO_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].code",
						hasItems("JERSEY-PRO", "PANTALONETA-ESTELAR", "T-SHIRT-RUNNING", "JERSEY-SLIM")))
				.andExpect(jsonPath("$[?(@.code == 'JERSEY-PRO')].series[0].measure").value("PRODUCCION"))
				.andExpect(jsonPath("$[?(@.code == 'JERSEY-PRO')].series[0].periodicity").value("MENSUAL"))
				.andExpect(jsonPath("$[?(@.code == 'JERSEY-PRO')].series[0].unit").value("UNIDADES"));
	}

	@Test
	void analystLoadsMonthsAndEveryoneReadsThemInOrder() throws Exception {
		long seriesId = newSeries();

		String created = upload(seriesId, """
				{"observations": [
					{"period": "2022-02", "value": 21},
					{"period": "2022-01", "value": 0},
					{"period": "2022-03", "value": 7.5}]}""")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/history/series/" + seriesId + "/observations"))
				.andExpect(jsonPath("$.observations").value(3))
				.andExpect(jsonPath("$.firstPeriod").value("2022-01"))
				.andExpect(jsonPath("$.lastPeriod").value("2022-03"))
				.andReturn().getResponse().getContentAsString();
		long loadId = Long.parseLong(created.replaceAll("(?s).*\"loadId\"\\s*:\\s*(\\d+).*", "$1"));

		MockHttpSession planner = login("planeador", DEMO_PASSWORD);
		mvc.perform(get("/api/v1/history/series/{id}/observations", seriesId).session(planner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dataVersion").value(loadId))
				.andExpect(jsonPath("$.observations[*].period", hasItems("2022-01", "2022-02", "2022-03")))
				.andExpect(jsonPath("$.observations[0].period").value("2022-01"))
				.andExpect(jsonPath("$.observations[0].value").value(0))
				.andExpect(jsonPath("$.observations[2].value").value(7.5));

		DataLoad load = loads.findById(loadId).orElseThrow();
		assertThat(load.getUserId()).isNotNull();
		assertThat(load.getCount()).isEqualTo(3);
	}

	@Test
	void nextMonthIsAppendedAndBecomesTheNewDataVersion() throws Exception {
		long seriesId = newSeries();
		upload(seriesId, """
				{"observations": [{"period": "2026-08", "value": 50}]}""").andExpect(status().isCreated());

		upload(seriesId, """
				{"observations": [{"period": "2026-09", "value": 155}]}""").andExpect(status().isCreated());

		mvc.perform(get("/api/v1/history/products").session(analyst()))
				.andExpect(jsonPath("$[?(@.series[0].id == %d)].series[0].observations".formatted(seriesId)).value(2))
				.andExpect(jsonPath("$[?(@.series[0].id == %d)].series[0].lastPeriod".formatted(seriesId))
						.value("2026-09"));
	}

	@Test
	void monthAlreadyLoadedIsAConflict() throws Exception {
		long seriesId = newSeries();
		upload(seriesId, """
				{"observations": [{"period": "2025-01", "value": 10}]}""").andExpect(status().isCreated());

		upload(seriesId, """
				{"observations": [{"period": "2025-01", "value": 12}]}""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("2025-01")));
	}

	@Test
	void rejectedBatchSavesNothing() throws Exception {
		long seriesId = newSeries();
		upload(seriesId, """
				{"observations": [{"period": "2025-01", "value": 10}]}""").andExpect(status().isCreated());

		upload(seriesId, """
				{"observations": [{"period": "2025-02", "value": 11}, {"period": "2025-04", "value": 13}]}""")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("business-rule"))
				.andExpect(jsonPath("$.detail", containsString("2025-03")));

		mvc.perform(get("/api/v1/history/series/{id}/observations", seriesId).session(analyst()))
				.andExpect(jsonPath("$.observations.length()").value(1));
	}

	@Test
	void futureMonthIsRejected() throws Exception {
		YearMonth next = YearMonth.now().plusMonths(2);
		upload(newSeries(), """
				{"observations": [{"period": "%s", "value": 1}]}""".formatted(next))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void invalidMonthsAreReportedPerField() throws Exception {
		upload(newSeries(), """
				{"observations": [
					{"period": "2026-13", "value": 1},
					{"period": "2026-01", "value": -5},
					{"period": "2026-02", "value": null},
					{"period": "2026-03", "value": 1.12345}]}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation"))
				.andExpect(jsonPath("$.errors[*].field", hasItems("observations[0].period",
						"observations[1].value", "observations[2].value", "observations[3].value")));
	}

	@Test
	void emptyBatchIsInvalid() throws Exception {
		upload(newSeries(), """
				{"observations": []}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("observations"));
	}

	@Test
	void unknownSeriesIsNotFound() throws Exception {
		mvc.perform(get("/api/v1/history/series/999999/observations").session(analyst()))
				.andExpect(status().isNotFound());
		upload(999999, """
				{"observations": [{"period": "2025-01", "value": 1}]}""").andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(authorities = "history:read")
	void loadRequiresWritePermissionAlsoInTheUseCase() {
		List<NewObservation> batch = List.of(new NewObservation(YearMonth.of(2025, 1), BigDecimal.ONE));
		assertThatThrownBy(() -> history.load(1L, batch, 1L))
				.isInstanceOf(AccessDeniedException.class);
	}

	private ResultActions upload(long seriesId, String json) throws Exception {
		return mvc.perform(post("/api/v1/history/series/{id}/observations", seriesId).session(analyst()).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private MockHttpSession analyst() throws Exception {
		if (analystSession == null) {
			analystSession = login("analista", DEMO_PASSWORD);
		}
		return analystSession;
	}
}
