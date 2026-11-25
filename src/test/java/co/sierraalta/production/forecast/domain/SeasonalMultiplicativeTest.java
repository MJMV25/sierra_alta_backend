package co.sierraalta.production.forecast.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import co.sierraalta.production.indicators.domain.ErrorIndicators;
import org.junit.jupiter.api.Test;

/**
 * Estacional multiplicativo contra las hojas "CASO 1/2 ESTACIONAL MULTIPLI." del Excel de la entrega 2
 * (ADR 12): cada pronóstico, el ciclo proyectado y MAD y MAPE sobre todos los periodos, con la tolerancia del
 * ADR 07.
 */
class SeasonalMultiplicativeTest {

	private static final double TOLERANCE = 1e-6;

	@Test
	void spokaneMatchesTheSpreadsheet() throws IOException {
		Golden golden = golden("seasonal-spokane.csv");
		Forecast forecast = new SeasonalMultiplicative(5).forecast(golden.actuals());

		assertThat(forecast.firstPeriod()).isZero();
		assertThat(forecast.fitted()).containsExactlyElementsOf(golden.forecasts());
		// Celda I3: primer periodo del ciclo proyectado (t = 31).
		assertThat(forecast.next()).isEqualTo(267.0);

		ErrorIndicators indicators = ErrorIndicators.of(forecast.pairs(golden.actuals(), 0));
		assertThat(indicators.mad().orElseThrow()).isCloseTo(13.033333333333333, within(TOLERANCE));
		assertThat(indicators.mape().orElseThrow()).isCloseTo(0.04684010977151084, within(TOLERANCE));
	}

	@Test
	void acmeMatchesTheSpreadsheet() throws IOException {
		Golden golden = golden("seasonal-acme.csv");
		Forecast forecast = new SeasonalMultiplicative(4).forecast(golden.actuals());

		assertThat(forecast.fitted()).containsExactlyElementsOf(golden.forecasts());
		// Celda I4: trimestre 1 de 2006.
		assertThat(forecast.next()).isEqualTo(794.0);

		ErrorIndicators indicators = ErrorIndicators.of(forecast.pairs(golden.actuals(), 0));
		assertThat(indicators.mad().orElseThrow()).isCloseTo(31.416666666666668, within(TOLERANCE));
		assertThat(indicators.mape().orElseThrow()).isCloseTo(0.08877871434121432, within(TOLERANCE));
	}

	@Test
	void incompleteLastCycleTakesTheProjectedCycle() throws IOException {
		Golden golden = golden("seasonal-acme.csv");
		List<Double> withExtraQuarters = new ArrayList<>(golden.actuals());
		withExtraQuarters.addAll(List.of(750.0, 500.0));
		Forecast forecast = new SeasonalMultiplicative(4).forecast(withExtraQuarters);

		// Los 24 primeros no cambian; 2006 T1 y T2 son las celdas I4 e I5, y el siguiente (T3) es I6.
		assertThat(forecast.fitted().subList(0, 24)).containsExactlyElementsOf(golden.forecasts());
		assertThat(forecast.at(24)).isEqualTo(794.0);
		assertThat(forecast.at(25)).isEqualTo(540.0);
		assertThat(forecast.next()).isEqualTo(394.0);
	}

	@Test
	void roundsHalvesAwayFromZeroLikeExcel() {
		assertThat(SeasonalMultiplicative.excelRound(282.5)).isEqualTo(283.0);
		assertThat(SeasonalMultiplicative.excelRound(282.49999999999997)).isEqualTo(283.0);
		assertThat(SeasonalMultiplicative.excelRound(282.4999)).isEqualTo(282.0);
	}

	@Test
	void doesNotNarrowTheCommonWindow() {
		List<Double> actuals = List.of(10.0, 20.0, 12.0, 22.0, 14.0, 24.0);
		Forecast seasonal = new SeasonalMultiplicative(2).forecast(actuals);
		Forecast sma = new SimpleMovingAverage(3).forecast(actuals);

		assertThat(Forecast.commonEvaluationStart(List.of(seasonal, sma))).isEqualTo(3);
	}

	@Test
	void requiresAtLeastTwoCompleteCycles() {
		assertThatThrownBy(() -> new SeasonalMultiplicative(4).forecast(List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)))
				.isInstanceOfSatisfying(InvalidForecastParameterException.class,
						ex -> assertThat(ex.field()).isEqualTo("seasonLength"))
				.hasMessageContaining("al menos 2 ciclos completos (8 datos)");
	}

	@Test
	void seasonLengthMustBeAtLeastTwo() {
		assertThatThrownBy(() -> new SeasonalMultiplicative(1))
				.isInstanceOfSatisfying(InvalidForecastParameterException.class,
						ex -> assertThat(ex.field()).isEqualTo("seasonLength"));
	}

	@Test
	void cycleWithoutProductionIsReportedInsteadOfDividingByZero() {
		assertThatThrownBy(() -> new SeasonalMultiplicative(2).forecast(List.of(0.0, 0.0, 5.0, 7.0)))
				.isInstanceOfSatisfying(InvalidForecastParameterException.class,
						ex -> assertThat(ex.field()).isEqualTo("series"))
				.hasMessageContaining("ciclo 1");
	}

	private record Golden(List<Double> actuals, List<Double> forecasts) {
	}

	private static Golden golden(String file) throws IOException {
		var resource = SeasonalMultiplicativeTest.class.getResourceAsStream("/forecast/golden/" + file);
		try (var input = Objects.requireNonNull(resource, file);
				var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			List<String[]> rows = reader.lines().filter(line -> !line.startsWith("#") && !line.startsWith("period"))
					.map(line -> line.split(",", -1))
					.toList();
			return new Golden(rows.stream().map(row -> Double.parseDouble(row[1])).toList(),
					rows.stream().map(row -> Double.parseDouble(row[2])).toList());
		}
	}
}
