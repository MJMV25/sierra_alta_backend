package co.sierraalta.production.forecast.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import co.sierraalta.production.indicators.domain.ErrorIndicators;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Casos de referencia del Excel corregido (EXCEL CORREGIDO DEL PROYECTO.xlsx, hojas CASO 1 SPOKANE y
 * CASO 2 ACME): cada pronóstico, MAD y MAPE deben coincidir en los primeros 6 decimales (respuesta 6
 * de Industrial). Los valores período a período están en {@code src/test/resources/forecast/golden}.
 */
class ExcelReferenceTest {

	private static final double TOLERANCE = 1e-6;

	private static final String[] METHODS = { "sma", "wma", "ses", "holt" };

	record ReferenceCase(String file, Map<String, ForecastMethod> methods, Map<String, double[]> madAndMape) {
	}

	static Stream<Arguments> cases() {
		Map<String, ForecastMethod> spokane = new LinkedHashMap<>();
		spokane.put("sma", new SimpleMovingAverage(5));
		spokane.put("wma", new WeightedMovingAverage(List.of(0.3, 0.2, 0.2, 0.2, 0.1)));
		spokane.put("ses", new ExponentialSmoothing(0.05));
		spokane.put("holt", new TrendAdjustedExponentialSmoothing(0.0025, 0.99));

		// Pesos K0..K3 tal como están en el Excel (K0 = dato más reciente); pregunta 2 aún abierta.
		Map<String, ForecastMethod> acme = new LinkedHashMap<>();
		acme.put("sma", new SimpleMovingAverage(4));
		acme.put("wma", new WeightedMovingAverage(List.of(0.1, 0.2, 0.3, 0.4)));
		acme.put("ses", new ExponentialSmoothing(0.6));
		acme.put("holt", new TrendAdjustedExponentialSmoothing(0.3, 0.1));

		return Stream.of(
				Arguments.of(new ReferenceCase("spokane.csv", spokane, Map.of(
						"sma", new double[] { 20.583999999999996, 0.075031047805661125 },
						"wma", new double[] { 19.800000000000008, 0.072199705350531052 },
						"ses", new double[] { 16.252331083901609, 0.058831943092208468 },
						"holt", new double[] { 16.060778575533561, 0.057923763097286106 }))),
				Arguments.of(new ReferenceCase("acme.csv", acme, Map.of(
						"sma", new double[] { 116.66666666666667, 0.29588619830216473 },
						"wma", new double[] { 106.9047619047619, 0.27273668130810985 },
						"ses", new double[] { 147.95104085609503, 0.37192699923148193 },
						"holt", new double[] { 132.38013472381994, 0.32096485371186784 }))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void reproducesEveryForecastOfTheSpreadsheet(ReferenceCase reference) throws IOException {
		List<String[]> rows = readRows(reference.file());
		List<Double> actuals = rows.stream().map(row -> Double.parseDouble(row[1])).toList();

		for (int column = 0; column < METHODS.length; column++) {
			String method = METHODS[column];
			Forecast forecast = reference.methods().get(method).forecast(actuals);
			for (int period = 0; period < rows.size(); period++) {
				String expected = rows.get(period)[column + 2];
				if (expected.isEmpty()) {
					assertThat(period).as("%s sin pronóstico en el periodo %d", method, period + 1)
							.isLessThan(forecast.firstPeriod());
				}
				else {
					assertThat(forecast.at(period)).as("%s periodo %d", method, period + 1)
							.isCloseTo(Double.parseDouble(expected), within(TOLERANCE));
				}
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void reproducesMadAndMapeOverTheCommonWindow(ReferenceCase reference) throws IOException {
		List<Double> actuals = readRows(reference.file()).stream().map(row -> Double.parseDouble(row[1])).toList();
		Map<String, Forecast> forecasts = new LinkedHashMap<>();
		reference.methods().forEach((method, algorithm) -> forecasts.put(method, algorithm.forecast(actuals)));
		int start = Forecast.commonEvaluationStart(forecasts.values());

		forecasts.forEach((method, forecast) -> {
			ErrorIndicators indicators = ErrorIndicators.of(forecast.pairs(actuals, start));
			double[] expected = reference.madAndMape().get(method);
			assertThat(indicators.mad().orElseThrow()).as("MAD %s", method).isCloseTo(expected[0], within(TOLERANCE));
			assertThat(indicators.mape().orElseThrow()).as("MAPE %s", method).isCloseTo(expected[1], within(TOLERANCE));
			assertThat(indicators.mapeExcluded()).isZero();
		});
	}

	private static List<String[]> readRows(String file) throws IOException {
		var resource = ExcelReferenceTest.class.getResourceAsStream("/forecast/golden/" + file);
		try (var input = Objects.requireNonNull(resource, file);
				var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			List<String[]> rows = new ArrayList<>();
			reader.lines().filter(line -> !line.startsWith("#") && !line.startsWith("period"))
					.forEach(line -> rows.add(line.split(",", -1)));
			return rows;
		}
	}
}
