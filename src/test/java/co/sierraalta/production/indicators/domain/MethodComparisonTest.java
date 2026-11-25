package co.sierraalta.production.indicators.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import co.sierraalta.production.indicators.domain.MethodComparison.MethodIndicators;
import co.sierraalta.production.indicators.domain.MethodComparison.Row;
import org.junit.jupiter.api.Test;

/** Reglas de la tabla comparativa (ADR 07 y 13). */
class MethodComparisonTest {

	@Test
	void eachIndicatorMarksItsOwnBest() {
		List<Row> rows = MethodComparison.of(List.of(
				indicators("A", "10", "0.20"),
				indicators("B", "12", "0.05")));

		assertThat(rows).extracting(Row::bestByMad).containsExactly(true, false);
		assertThat(rows).extracting(Row::bestByMape).containsExactly(false, true);
	}

	@Test
	void valuesEqualToSixDecimalsTieAndNotCalculableNeverWins() {
		List<Row> rows = MethodComparison.of(List.of(
				indicators("A", "16.0607781", "0.05"),
				indicators("B", "16.0607779", "0.06"),
				indicators("C", "16.060779", "0.07"),
				new MethodIndicators("D", null, null, 0, 0, true)));

		assertThat(rows).extracting(Row::bestByMad).containsExactly(true, true, false, false);
		assertThat(rows).extracting(Row::bestByMape).containsExactly(true, false, false, false);
	}

	@Test
	void aMethodThatIsNotComparableIsListedButNeverTheBest() {
		List<Row> rows = MethodComparison.of(List.of(
				indicators("A", "20", "0.10"),
				new MethodIndicators("ESTACIONAL", new BigDecimal("5"), new BigDecimal("0.01"), 5, 0, false)));

		assertThat(rows).extracting(Row::bestByMad).containsExactly(true, false);
		assertThat(rows).extracting(Row::bestByMape).containsExactly(true, false);
	}

	private static MethodIndicators indicators(String method, String mad, String mape) {
		return new MethodIndicators(method, new BigDecimal(mad), new BigDecimal(mape), 5, 0, true);
	}
}
