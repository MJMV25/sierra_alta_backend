package co.sierraalta.production.forecast;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import co.sierraalta.production.ApiIntegrationTest;
import co.sierraalta.production.forecast.application.ForecastCommand;
import co.sierraalta.production.forecast.application.ForecastService;
import co.sierraalta.production.forecast.application.MethodSpec;
import co.sierraalta.production.forecast.domain.MethodCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Pronóstico por la API real: la serie se carga con el endpoint de históricos y el resultado debe coincidir
 * con el Excel corregido a 6 decimales (respuesta 6). Cada prueba usa su propia serie.
 */
class ForecastApiTest extends ApiIntegrationTest {

	private static final double EXCEL = 1e-6;

	/** MAPE en porcentaje: la tolerancia de 6 decimales de la fracción, × 100. */
	private static final double EXCEL_PERCENT = 1e-4;

	private static final YearMonth FIRST_MONTH = YearMonth.of(2020, 1);

	private static final String SPOKANE_METHODS = """
			[{"method": "SIMPLE_MOVING_AVERAGE", "periods": 5},
			 {"method": "WEIGHTED_MOVING_AVERAGE", "weights": [0.3, 0.2, 0.2, 0.2, 0.1]},
			 {"method": "EXPONENTIAL_SMOOTHING", "alpha": 0.05},
			 {"method": "TREND_ADJUSTED_EXPONENTIAL_SMOOTHING", "alpha": 0.0025, "beta": 0.99}]""";

	@Autowired
	ForecastService forecasts;

	/** Sesiones abiertas al primer uso: un login previo borraría el contexto de {@code @WithMockUser}. */
	private MockHttpSession analystSession;
	private MockHttpSession plannerSession;

	@Test
	void spokaneThroughTheApiMatchesTheSpreadsheet() throws Exception {
		long seriesId = seriesWith(golden("spokane.csv"));

		String created = execute(seriesId, SPOKANE_METHODS)
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.evaluationStart").value("2020-06"))
				.andExpect(jsonPath("$.calculationVersion").value("1.0"))
				.andExpect(jsonPath("$.measure").value("PRODUCCION"))
				.andExpect(jsonPath("$.actuals.length()").value(30))
				.andExpect(jsonPath("$.methods[0].method").value("SIMPLE_MOVING_AVERAGE"))
				.andExpect(jsonPath("$.methods[0].mad", closeTo(20.583999999999996, EXCEL)))
				.andExpect(jsonPath("$.methods[1].mad", closeTo(19.800000000000008, EXCEL)))
				.andExpect(jsonPath("$.methods[2].mad", closeTo(16.252331083901609, EXCEL)))
				.andExpect(jsonPath("$.methods[3].mad", closeTo(16.060778575533561, EXCEL)))
				.andExpect(jsonPath("$.methods[0].mapePercent", closeTo(7.5031047805661125, EXCEL_PERCENT)))
				.andExpect(jsonPath("$.methods[1].mapePercent", closeTo(7.2199705350531052, EXCEL_PERCENT)))
				.andExpect(jsonPath("$.methods[2].mapePercent", closeTo(5.8831943092208468, EXCEL_PERCENT)))
				.andExpect(jsonPath("$.methods[3].mapePercent", closeTo(5.7923763097286106, EXCEL_PERCENT)))
				.andExpect(jsonPath("$.methods[0].pairsUsed").value(25))
				.andExpect(jsonPath("$.methods[2].pairsUsed").value(25))
				// El suavizamiento pronostica desde el periodo 2 aunque se evalúe desde el 6 (respuesta 7).
				.andExpect(jsonPath("$.methods[0].forecasts.length()").value(25))
				.andExpect(jsonPath("$.methods[2].forecasts.length()").value(29))
				.andExpect(jsonPath("$.methods[0].forecasts[0].period").value("2020-06"))
				.andExpect(jsonPath("$.methods[0].forecasts[0].forecast", closeTo(289.8, EXCEL)))
				.andExpect(jsonPath("$.methods[0].forecasts[0].actual").value(268))
				.andExpect(jsonPath("$.methods[1].weights[0]").value(0.3))
				.andExpect(jsonPath("$.methods[3].alpha").value(0.0025))
				.andExpect(jsonPath("$.methods[3].nextForecast.period").value("2022-07"))
				.andExpect(jsonPath("$.methods[3].nextForecast.actual").value(nullValue()))
				.andReturn().getResponse().getContentAsString();
		long id = idOf(created);

		// Lo guardado se lee igual, también por el supervisor (solo lectura).
		mvc.perform(get("/api/v1/forecasts/{id}", id).session(login("supervisor", DEMO_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.methods[2].mad", closeTo(16.252331083901609, EXCEL)))
				.andExpect(jsonPath("$.methods[3].mapePercent", closeTo(5.7923763097286106, EXCEL_PERCENT)));
		mvc.perform(get("/api/v1/forecasts").param("seriesId", String.valueOf(seriesId)).session(planner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(id))
				.andExpect(jsonPath("$[0].methods.length()").value(4));
	}

	@Test
	void acmeThroughTheApiMatchesTheSpreadsheet() throws Exception {
		// Pesos tal como están en el Excel (pregunta 2 aún abierta con Industrial).
		execute(seriesWith(golden("acme.csv")), """
				[{"method": "SIMPLE_MOVING_AVERAGE", "periods": 4},
				 {"method": "WEIGHTED_MOVING_AVERAGE", "weights": [0.1, 0.2, 0.3, 0.4]},
				 {"method": "EXPONENTIAL_SMOOTHING", "alpha": 0.6},
				 {"method": "TREND_ADJUSTED_EXPONENTIAL_SMOOTHING", "alpha": 0.3, "beta": 0.1}]""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.evaluationStart").value("2020-05"))
				.andExpect(jsonPath("$.methods[0].mad", closeTo(116.66666666666667, EXCEL)))
				.andExpect(jsonPath("$.methods[1].mad", closeTo(106.9047619047619, EXCEL)))
				.andExpect(jsonPath("$.methods[2].mad", closeTo(147.95104085609503, EXCEL)))
				.andExpect(jsonPath("$.methods[3].mad", closeTo(132.38013472381994, EXCEL)))
				.andExpect(jsonPath("$.methods[1].mapePercent", closeTo(27.273668130810985, EXCEL_PERCENT)));
	}

	@Test
	void sameIdempotencyKeyReturnsTheOriginalExecution() throws Exception {
		long seriesId = seriesWith(List.of(10.0, 20.0, 30.0));
		String key = UUID.randomUUID().toString();
		String body = """
				{"seriesId": %d, "methods": [{"method": "SIMPLE_MOVING_AVERAGE", "periods": 2}]}""".formatted(seriesId);

		String created = postForecast(body, key).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long first = idOf(created);
		postForecast(body, key)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(first));
		postForecast(body, UUID.randomUUID().toString()).andExpect(status().isCreated());
	}

	@Test
	void periodsEqualToTheDataGiveOnlyTheNextForecast() throws Exception {
		execute(seriesWith(List.of(100.0, 120.0, 110.0, 130.0)), """
				[{"method": "SIMPLE_MOVING_AVERAGE", "periods": 4}]""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.evaluationStart").value(nullValue()))
				.andExpect(jsonPath("$.methods[0].mad").value(nullValue()))
				.andExpect(jsonPath("$.methods[0].mapePercent").value(nullValue()))
				.andExpect(jsonPath("$.methods[0].pairsUsed").value(0))
				.andExpect(jsonPath("$.methods[0].nextForecast.forecast").value(115));
	}

	@Test
	void zeroActualsAreExcludedFromMapeAndCounted() throws Exception {
		execute(seriesWith(List.of(0.0, 10.0, 0.0, 20.0)), """
				[{"method": "SIMPLE_MOVING_AVERAGE", "periods": 1}]""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.methods[0].pairsUsed").value(3))
				.andExpect(jsonPath("$.methods[0].mapeExcluded").value(1))
				.andExpect(jsonPath("$.methods[0].mad", closeTo((10 + 10 + 20) / 3.0, EXCEL)))
				.andExpect(jsonPath("$.methods[0].mapePercent").value(100));
	}

	@Test
	void invalidParametersAreReportedOnTheirField() throws Exception {
		long seriesId = seriesWith(List.of(1.0, 2.0, 3.0));
		expectFieldError(seriesId, """
				[{"method": "EXPONENTIAL_SMOOTHING", "alpha": 1}]""", "methods[0].alpha");
		expectFieldError(seriesId, """
				[{"method": "SIMPLE_MOVING_AVERAGE", "periods": 2},
				 {"method": "WEIGHTED_MOVING_AVERAGE", "weights": [0.5, 0.4]}]""", "methods[1].weights");
		expectFieldError(seriesId, """
				[{"method": "SIMPLE_MOVING_AVERAGE", "periods": 4}]""", "methods[0].periods");
		expectFieldError(seriesId, """
				[{"method": "TREND_ADJUSTED_EXPONENTIAL_SMOOTHING", "alpha": 0.5}]""", "methods[0].beta");
		expectFieldError(seriesId, """
				[{"method": "EXPONENTIAL_SMOOTHING", "alpha": 0.5},
				 {"method": "EXPONENTIAL_SMOOTHING", "alpha": 0.2}]""", "methods[1].method");
		expectFieldError(seriesId, "[]", "methods");
		postForecast("""
				{"seriesId": %d, "methods": [{"method": "EXPONENTIAL_SMOOTHING", "alpha": 0.5}]}""".formatted(seriesId),
				"clave con espacios")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"));
	}

	@Test
	void seriesWithoutDataCannotBeForecast() throws Exception {
		execute(newSeries(), """
				[{"method": "EXPONENTIAL_SMOOTHING", "alpha": 0.5}]""")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("business-rule"));
	}

	@Test
	void unknownSeriesOrExecutionIsNotFound() throws Exception {
		execute(999999, """
				[{"method": "EXPONENTIAL_SMOOTHING", "alpha": 0.5}]""").andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/forecasts/999999").session(planner())).andExpect(status().isNotFound());
	}

	@Test
	void listingWithoutFilterIncludesRecentExecutions() throws Exception {
		long seriesId = seriesWith(List.of(5.0, 6.0));
		long id = idOf(execute(seriesId, """
				[{"method": "EXPONENTIAL_SMOOTHING", "alpha": 0.5}]""")
				.andReturn().getResponse().getContentAsString());

		mvc.perform(get("/api/v1/forecasts").session(planner()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem((int) id)));
	}

	@Test
	@WithMockUser(authorities = { "forecast:read", "history:read" })
	void executeRequiresItsPermissionAlsoInTheUseCase() {
		ForecastCommand command = new ForecastCommand(1L,
				List.of(new MethodSpec(MethodCode.EXPONENTIAL_SMOOTHING, null, null, BigDecimal.ONE, null)));
		assertThatThrownBy(() -> forecasts.execute(command, 1L, null)).isInstanceOf(AccessDeniedException.class);
	}

	private void expectFieldError(long seriesId, String methods, String field) throws Exception {
		execute(seriesId, methods)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation"))
				.andExpect(jsonPath("$.errors[0].field").value(field));
	}

	private ResultActions execute(long seriesId, String methods) throws Exception {
		return postForecast("""
				{"seriesId": %d, "methods": %s}""".formatted(seriesId, methods), null);
	}

	private ResultActions postForecast(String body, String idempotencyKey) throws Exception {
		var request = post("/api/v1/forecasts")
				.session(planner()).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
		if (idempotencyKey != null) {
			request.header("Idempotency-Key", idempotencyKey);
		}
		return mvc.perform(request);
	}

	/** Serie nueva con estos valores desde enero de 2020, cargada por el analista con la API de históricos. */
	private long seriesWith(List<Double> values) throws Exception {
		long seriesId = newSeries();
		String observations = IntStream.range(0, values.size())
				.mapToObj(i -> "{\"period\": \"%s\", \"value\": %s}"
						.formatted(FIRST_MONTH.plusMonths(i), values.get(i)))
				.collect(Collectors.joining(","));
		mvc.perform(post("/api/v1/history/series/{id}/observations", seriesId).session(analyst()).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"observations\": [" + observations + "]}"))
				.andExpect(status().isCreated());
		return seriesId;
	}

	private static List<Double> golden(String file) throws IOException {
		var resource = ForecastApiTest.class.getResourceAsStream("/forecast/golden/" + file);
		try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(resource, file),
				StandardCharsets.UTF_8))) {
			return reader.lines().filter(line -> !line.startsWith("#") && !line.startsWith("period"))
					.map(line -> Double.parseDouble(line.split(",")[1]))
					.toList();
		}
	}

	private static long idOf(String json) {
		return Long.parseLong(json.replaceAll("(?s)^\\{\"id\"\\s*:\\s*(\\d+).*", "$1"));
	}

	private MockHttpSession analyst() throws Exception {
		if (analystSession == null) {
			analystSession = login("analista", DEMO_PASSWORD);
		}
		return analystSession;
	}

	private MockHttpSession planner() throws Exception {
		if (plannerSession == null) {
			plannerSession = login("planeador", DEMO_PASSWORD);
		}
		return plannerSession;
	}
}
