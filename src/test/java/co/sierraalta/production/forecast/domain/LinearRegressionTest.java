package co.sierraalta.production.forecast.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import co.sierraalta.production.indicators.domain.ErrorIndicators;
import org.junit.jupiter.api.Test;

/**
 * Regresión lineal con la corrección de Industrial (documento "Error en el proyecto", 2026-11-06): la
 * pendiente es la celda PENDIENTE del Excel y el intercepto el corregido (284,84 y 251,5). MAD y MAPE se
 * evalúan en la ventana común con los otros 4 métodos (Spokane desde t = 6, ACME desde t = 5); el Excel de la
 * entrega 2 ya trae la corrección y sus valores exactos (15,897 · 5,79 % y 118,399 · 35,94 %) se exigen con 6
 * decimales.
 */
class LinearRegressionTest {

	private static final double TOLERANCE = 1e-6;

	/** Los valores del documento vienen con 2 decimales. */
	private static final double DOCUMENT = 0.005;

	@Test
	void spokaneMatchesTheCorrectedSpreadsheet() throws IOException {
		List<Double> actuals = actuals("spokane.csv");
		Forecast forecast = new LinearRegression().forecast(actuals);

		double slope = -0.18131256952169078;
		double intercept = 284.8436781609196;
		assertThat(forecast.firstPeriod()).isZero();
		assertThat(forecast.fitted()).hasSize(30);
		assertThat(forecast.at(0)).isCloseTo(slope * 1 + intercept, within(TOLERANCE));
		assertThat(forecast.at(29)).isCloseTo(slope * 30 + intercept, within(TOLERANCE));
		assertThat(forecast.next()).isCloseTo(slope * 31 + intercept, within(TOLERANCE));
		// Primera fila de la imagen del documento: P = 284,7 y |E| = 9,66.
		assertThat(Math.abs(actuals.get(0) - forecast.at(0))).isCloseTo(9.66, within(DOCUMENT));

		ErrorIndicators indicators = ErrorIndicators.of(forecast.pairs(actuals, 5));
		assertThat(indicators.mad().orElseThrow()).isCloseTo(15.896978865406004, within(TOLERANCE));
		assertThat(indicators.mape().orElseThrow()).isCloseTo(0.057934757119589786, within(TOLERANCE));
	}

	@Test
	void acmeMatchesTheCorrectedSpreadsheet() throws IOException {
		List<Double> actuals = actuals("acme.csv");
		Forecast forecast = new LinearRegression().forecast(actuals);

		assertThat(forecast.at(0)).isCloseTo(13.423076923076923 + 251.5, within(TOLERANCE));
		assertThat(forecast.next()).isCloseTo(600.5, within(TOLERANCE));

		ErrorIndicators indicators = ErrorIndicators.of(forecast.pairs(actuals, 4));
		assertThat(indicators.mad().orElseThrow()).isCloseTo(118.39926739926739, within(TOLERANCE));
		assertThat(indicators.mape().orElseThrow()).isCloseTo(0.3593982973979742, within(TOLERANCE));
	}

	@Test
	void doesNotNarrowTheCommonWindow() {
		List<Double> actuals = List.of(10.0, 20.0, 30.0, 40.0);
		Forecast regression = new LinearRegression().forecast(actuals);
		Forecast sma = new SimpleMovingAverage(2).forecast(actuals);

		assertThat(Forecast.commonEvaluationStart(List.of(regression, sma))).isEqualTo(2);
		assertThat(regression.fitted()).containsExactly(10.0, 20.0, 30.0, 40.0);
		assertThat(regression.next()).isCloseTo(50.0, within(TOLERANCE));
	}

	@Test
	void flatSeriesHasZeroSlope() {
		Forecast forecast = new LinearRegression().forecast(List.of(7.0, 7.0, 7.0));
		assertThat(forecast.next()).isCloseTo(7.0, within(TOLERANCE));
	}

	@Test
	void needsAtLeastTwoData() {
		assertThatThrownBy(() -> new LinearRegression().forecast(List.of(5.0)))
				.isInstanceOf(InvalidForecastParameterException.class)
				.hasMessageContaining("al menos 2 datos");
	}

	private static List<Double> actuals(String file) throws IOException {
		var resource = LinearRegressionTest.class.getResourceAsStream("/forecast/golden/" + file);
		try (var input = Objects.requireNonNull(resource, file);
				var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().filter(line -> !line.startsWith("#") && !line.startsWith("period"))
					.map(line -> Double.parseDouble(line.split(",", -1)[1]))
					.toList();
		}
	}
}
