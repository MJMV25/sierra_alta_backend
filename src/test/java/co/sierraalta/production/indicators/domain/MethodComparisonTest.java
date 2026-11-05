package co.sierraalta.production.indicators.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import co.sierraalta.production.indicators.domain.MethodComparison.MethodIndicators;
import co.sierraalta.production.indicators.domain.MethodComparison.Row;
import org.junit.jupiter.api.Test;

/** Reglas de la tabla comparativa (ADR 07, preguntas 21 y 22). */
class MethodComparisonTest {

	private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");

	@Test
	void mapeBelowTheThresholdMeetsItAndEqualOrAboveDoesNot() {
		assertThat(MethodComparison.status(new BigDecimal("0.0999999999"), TEN_PERCENT)).isEqualTo(AcceptanceStatus.CUMPLE);
		assertThat(MethodComparison.status(new BigDecimal("0.1000000000"), TEN_PERCENT)).isEqualTo(AcceptanceStatus.NO_CUMPLE);
		assertThat(MethodComparison.status(new BigDecimal("0.25"), TEN_PERCENT)).isEqualTo(AcceptanceStatus.NO_CUMPLE);
	}

	@Test
	void missingMapeOrThresholdIsReportedInsteadOfGuessed() {
		assertThat(MethodComparison.status(null, TEN_PERCENT)).isEqualTo(AcceptanceStatus.SIN_EVALUAR);
		assertThat(MethodComparison.status(new BigDecimal("0.05"), null)).isEqualTo(AcceptanceStatus.SIN_CRITERIO);
	}

	@Test
	void eachIndicatorMarksItsOwnBestWithoutASingleWinner() {
		List<Row> rows = MethodComparison.of(TEN_PERCENT, List.of(
				indicators("A", "10", "0.20"),
				indicators("B", "12", "0.05")));

		assertThat(rows).extracting(Row::bestByMad).containsExactly(true, false);
		assertThat(rows).extracting(Row::bestByMape).containsExactly(false, true);
		assertThat(rows).extracting(Row::status).containsExactly(AcceptanceStatus.NO_CUMPLE, AcceptanceStatus.CUMPLE);
	}

	@Test
	void valuesEqualToSixDecimalsTieAndNotCalculableNeverWins() {
		List<Row> rows = MethodComparison.of(TEN_PERCENT, List.of(
				indicators("A", "16.0607781", "0.05"),
				indicators("B", "16.0607779", "0.06"),
				indicators("C", "16.060779", "0.07"),
				new MethodIndicators("D", null, null, 0, 0)));

		assertThat(rows).extracting(Row::bestByMad).containsExactly(true, true, false, false);
		assertThat(rows.get(3).status()).isEqualTo(AcceptanceStatus.SIN_EVALUAR);
	}

	private static MethodIndicators indicators(String method, String mad, String mape) {
		return new MethodIndicators(method, new BigDecimal(mad), new BigDecimal(mape), 5, 0);
	}
}
