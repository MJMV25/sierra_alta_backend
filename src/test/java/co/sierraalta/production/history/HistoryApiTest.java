package co.sierraalta.production.history;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.ApiIntegrationTest;
import co.sierraalta.production.history.application.HistoryService;
import co.sierraalta.production.history.application.SeriesEdit;
import co.sierraalta.production.history.domain.ObservationChange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Hoja de datos por la API real (ADR 13). Cada prueba usa su propia serie para no depender del orden de
 * ejecución; las 4 referencias de la empresa quedan vacías.
 */
class HistoryApiTest extends ApiIntegrationTest {

	@Autowired
	HistoryService history;

	/** Sesiones abiertas al primer uso: un login previo borraría el contexto de {@code @WithMockUser}. */
	private MockHttpSession adminSession;
	private MockHttpSession plannerSession;

	@Test
	void catalogHasTheCompanyReferencesAndTheExcelCasesAsSamples() throws Exception {
		mvc.perform(get("/api/v1/history/products").session(planner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].code", hasItems("JERSEY-PRO", "PANTALONETA-ESTELAR", "T-SHIRT-RUNNING",
						"JERSEY-SLIM", "CASO-1-SPOKANE", "CASO-2-ACME")))
				.andExpect(jsonPath("$[?(@.code == 'JERSEY-PRO')].sample").value(false))
				.andExpect(jsonPath("$[?(@.code == 'JERSEY-PRO')].series[0].measure").value("PRODUCCION"))
				.andExpect(jsonPath("$[?(@.code == 'JERSEY-PRO')].series[0].periodicity").value("MENSUAL"))
				.andExpect(jsonPath("$[?(@.code == 'CASO-1-SPOKANE')].sample").value(true))
				.andExpect(jsonPath("$[?(@.code == 'CASO-1-SPOKANE')].series[0].periodicity").value("PERIODO"))
				.andExpect(jsonPath("$[?(@.code == 'CASO-1-SPOKANE')].series[0].unit").value("GALONES"))
				.andExpect(jsonPath("$[?(@.code == 'CASO-1-SPOKANE')].series[0].observations").value(30))
				.andExpect(jsonPath("$[?(@.code == 'CASO-1-SPOKANE')].series[0].lastPeriod").value("2002-06"))
				.andExpect(jsonPath("$[?(@.code == 'CASO-2-ACME')].series[0].observations").value(25));

		// Valores de la hoja DATOS: t = 1 se guarda como 2000-01.
		mvc.perform(get("/api/v1/history/series/{id}/observations", excelCase("CASO-2-ACME")).session(planner()))
				.andExpect(jsonPath("$.observations[0].period").value("2000-01"))
				.andExpect(jsonPath("$.observations[0].value").value(500))
				.andExpect(jsonPath("$.observations[24].value").value(850));
	}

	@Test
	void sampleDataCannotBeEdited() throws Exception {
		save(sheet(excelCase("CASO-1-SPOKANE"), null, "{\"period\": \"2002-07\", \"value\": 300}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail", containsString("ejemplo")));
	}

	@Test
	void adminAddsMonthsAndEveryoneReadsThemInOrder() throws Exception {
		long seriesId = newSeries();

		String saved = save(sheet(seriesId, null, """
				{"period": "2022-02", "value": 21},
				{"period": "2022-01", "value": 0},
				{"period": "2022-03", "value": 7}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].seriesId").value(seriesId))
				.andExpect(jsonPath("$[0].added").value(3))
				.andExpect(jsonPath("$[0].corrected").value(0))
				.andExpect(jsonPath("$[0].dataVersion").isNumber())
				.andReturn().getResponse().getContentAsString();
		long version = dataVersionOf(saved);

		mvc.perform(get("/api/v1/history/series/{id}/observations", seriesId).session(planner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dataVersion").value(version))
				.andExpect(jsonPath("$.observations[0].period").value("2022-01"))
				.andExpect(jsonPath("$.observations[0].value").value(0))
				.andExpect(jsonPath("$.observations[2].value").value(7));
	}

	@Test
	void correctionsAndRemovalsKeepAHistoryAndAdvanceTheVersion() throws Exception {
		long seriesId = newSeries();
		long first = dataVersionOf(save(sheet(seriesId, null, """
				{"period": "2025-01", "value": 10},
				{"period": "2025-02", "value": 20},
				{"period": "2025-03", "value": 30}""")).andReturn().getResponse().getContentAsString());

		String saved = save(sheet(seriesId, first, """
				{"period": "2025-02", "value": 25},
				{"period": "2025-03", "value": null},
				{"period": "2025-01", "value": 10}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].added").value(0))
				.andExpect(jsonPath("$[0].corrected").value(1))
				.andExpect(jsonPath("$[0].removed").value(1))
				.andExpect(jsonPath("$[0].dataVersion", greaterThan((int) first)))
				.andReturn().getResponse().getContentAsString();

		mvc.perform(get("/api/v1/history/series/{id}/observations", seriesId).session(planner()))
				.andExpect(jsonPath("$.dataVersion").value(dataVersionOf(saved)))
				.andExpect(jsonPath("$.observations.length()").value(2))
				.andExpect(jsonPath("$.observations[1].value").value(25));

		// Del guardado más reciente al más antiguo; enero no cambió y no aparece dos veces.
		mvc.perform(get("/api/v1/history/series/{id}/changes", seriesId).session(planner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[0].period").value("2025-02"))
				.andExpect(jsonPath("$[0].previousValue").value(20))
				.andExpect(jsonPath("$[0].newValue").value(25))
				.andExpect(jsonPath("$[0].changedBy").value(ADMIN))
				.andExpect(jsonPath("$[1].period").value("2025-03"))
				.andExpect(jsonPath("$[1].previousValue").value(30))
				.andExpect(jsonPath("$[1].newValue").doesNotExist())
				.andExpect(jsonPath("$[2].period").value("2025-01"))
				.andExpect(jsonPath("$[2].previousValue").doesNotExist());
	}

	@Test
	void savingWithoutRealChangesKeepsTheVersion() throws Exception {
		long seriesId = newSeries();
		long version = dataVersionOf(save(sheet(seriesId, null, "{\"period\": \"2025-01\", \"value\": 10}"))
				.andReturn().getResponse().getContentAsString());

		save(sheet(seriesId, version, "{\"period\": \"2025-01\", \"value\": 10}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].dataVersion").value(version))
				.andExpect(jsonPath("$[0].corrected").value(0));
	}

	@Test
	void staleSheetIsAConflict() throws Exception {
		long seriesId = newSeries();
		long version = dataVersionOf(save(sheet(seriesId, null, "{\"period\": \"2025-01\", \"value\": 10}"))
				.andReturn().getResponse().getContentAsString());
		save(sheet(seriesId, version, "{\"period\": \"2025-02\", \"value\": 11}")).andExpect(status().isOk());

		save(sheet(seriesId, version, "{\"period\": \"2025-01\", \"value\": 12}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("Recargue")));
	}

	@Test
	void rejectedSaveChangesNoSeries() throws Exception {
		long valid = newSeries();
		long invalid = newSeries();
		save(invalid, "{\"period\": \"2025-01\", \"value\": 10}");

		save("""
				{"series": [
					{"seriesId": %d, "changes": [{"period": "2025-01", "value": 5}]},
					{"seriesId": %d, "changes": [{"period": "2025-03", "value": 13}]}]}""".formatted(valid, invalid))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("business-rule"))
				.andExpect(jsonPath("$.detail", containsString("2025-02")));

		mvc.perform(get("/api/v1/history/series/{id}/observations", valid).session(planner()))
				.andExpect(jsonPath("$.observations.length()").value(0));
	}

	@Test
	void futureMonthIsRejected() throws Exception {
		save(sheet(newSeries(), null, "{\"period\": \"%s\", \"value\": 1}".formatted(YearMonth.now().plusMonths(2))))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void invalidMonthsAreReportedPerField() throws Exception {
		save(sheet(newSeries(), null, """
				{"period": "2026-13", "value": 1},
				{"period": "2026-01", "value": -5},
				{"period": "2026-03", "value": 7.5}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation"))
				.andExpect(jsonPath("$.errors[*].field", hasItems("series[0].changes[0].period",
						"series[0].changes[1].value", "series[0].changes[2].value")));
	}

	@Test
	void repeatedSeriesAndEmptyChangesAreInvalid() throws Exception {
		long seriesId = newSeries();
		save("""
				{"series": [
					{"seriesId": %d, "changes": [{"period": "2025-01", "value": 1}]},
					{"seriesId": %d, "changes": [{"period": "2025-02", "value": 1}]}]}""".formatted(seriesId, seriesId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("series[1].seriesId"));
		save(sheet(seriesId, null, ""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("series[0].changes"));
	}

	@Test
	void unknownSeriesIsNotFound() throws Exception {
		mvc.perform(get("/api/v1/history/series/999999/observations").session(planner()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/history/series/999999/changes").session(planner()))
				.andExpect(status().isNotFound());
		save(sheet(999999, null, "{\"period\": \"2025-01\", \"value\": 1}")).andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(authorities = "history:read")
	void editingRequiresWritePermissionAlsoInTheUseCase() {
		List<SeriesEdit> edits = List.of(new SeriesEdit(1L, null,
				List.of(new ObservationChange(YearMonth.of(2025, 1), BigDecimal.ONE))));
		assertThatThrownBy(() -> history.edit(edits, 1L)).isInstanceOf(AccessDeniedException.class);
	}

	private void save(long seriesId, String changes) throws Exception {
		save(sheet(seriesId, null, changes)).andExpect(status().isOk());
	}

	private ResultActions save(String json) throws Exception {
		return mvc.perform(patch("/api/v1/history/observations").session(admin()).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private static String sheet(long seriesId, Long dataVersion, String changes) {
		return """
				{"series": [{"seriesId": %d, "dataVersion": %s, "changes": [%s]}]}"""
				.formatted(seriesId, dataVersion, changes);
	}

	private static long dataVersionOf(String json) {
		return Long.parseLong(json.replaceAll("(?s).*\"dataVersion\"\\s*:\\s*(\\d+).*", "$1"));
	}

	private MockHttpSession admin() throws Exception {
		if (adminSession == null) {
			adminSession = login(ADMIN, ADMIN_PASSWORD);
		}
		return adminSession;
	}

	private MockHttpSession planner() throws Exception {
		if (plannerSession == null) {
			plannerSession = login("planeador", DEMO_PASSWORD);
		}
		return plannerSession;
	}
}
