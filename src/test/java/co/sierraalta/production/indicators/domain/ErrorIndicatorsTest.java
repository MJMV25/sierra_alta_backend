package co.sierraalta.production.indicators.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Regla del MAPE con reales en cero (respuesta 8 de Industrial) e indicadores no calculables. */
class ErrorIndicatorsTest {

	@Test
	void zeroActualsAreExcludedOnlyFromMape() {
		ErrorIndicators indicators = ErrorIndicators.of(List.of(
				new ForecastPair(0, 10), new ForecastPair(50, 40), new ForecastPair(0.5, 1)));

		assertThat(indicators.mad().orElseThrow()).isCloseTo((10 + 10 + 0.5) / 3, within(1e-12));
		assertThat(indicators.mape().orElseThrow()).isCloseTo((0.2 + 1.0) / 2, within(1e-12));
		assertThat(indicators.pairsUsed()).isEqualTo(3);
		assertThat(indicators.mapeExcluded()).isEqualTo(1);
	}

	@Test
	void mapeIsNotCalculableWhenEveryActualIsZero() {
		ErrorIndicators indicators = ErrorIndicators.of(List.of(new ForecastPair(0, 3), new ForecastPair(0, 0)));

		assertThat(indicators.mad()).hasValue(1.5);
		assertThat(indicators.mape()).isEmpty();
		assertThat(indicators.mapeExcluded()).isEqualTo(2);
	}

	@Test
	void noPairsMeansNothingIsCalculable() {
		ErrorIndicators indicators = ErrorIndicators.of(List.of());

		assertThat(indicators.mad()).isEmpty();
		assertThat(indicators.mape()).isEmpty();
		assertThat(indicators.pairsUsed()).isZero();
	}
}
