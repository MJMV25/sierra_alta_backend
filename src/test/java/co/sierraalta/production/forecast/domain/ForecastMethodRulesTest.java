package co.sierraalta.production.forecast.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;

import co.sierraalta.production.indicators.domain.ErrorIndicators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reglas de parámetros y casos borde acordados con Industrial (ADR 07). */
class ForecastMethodRulesTest {

	private static final List<Double> SERIES = List.of(100.0, 120.0, 110.0, 130.0);

	@Test
	void simpleMovingAverageMatchesTheAdr04Reference() {
		Forecast forecast = new SimpleMovingAverage(2).forecast(SERIES);

		assertThat(forecast.fitted()).containsExactly(110.0, 115.0);
		assertThat(forecast.next()).isEqualTo(120.0);
		ErrorIndicators indicators = ErrorIndicators.of(forecast.pairs(SERIES, forecast.firstPeriod()));
		assertThat(indicators.mad()).hasValue(7.5);
		assertThat(indicators.mape().orElseThrow()).isCloseTo(0.05769231, within(1e-6));
	}

	@Test
	void periodsEqualToSeriesLengthGivesOnlyTheNextForecast() {
		Forecast forecast = new SimpleMovingAverage(4).forecast(SERIES);

		assertThat(forecast.fitted()).isEmpty();
		assertThat(forecast.next()).isEqualTo(115.0);
		ErrorIndicators indicators = ErrorIndicators.of(forecast.pairs(SERIES, forecast.firstPeriod()));
		assertThat(indicators.mad()).isEmpty();
		assertThat(indicators.mape()).isEmpty();
	}

	@Test
	void periodsGreaterThanSeriesLengthIsRejected() {
		assertThatThrownBy(() -> new SimpleMovingAverage(5).forecast(SERIES))
				.isInstanceOf(InvalidForecastParameterException.class)
				.extracting("field").isEqualTo("periods");
	}

	@Test
	void periodsMustBePositive() {
		assertThatThrownBy(() -> new SimpleMovingAverage(0)).isInstanceOf(InvalidForecastParameterException.class);
	}

	@Test
	void weightsMustSumOne() {
		assertThatThrownBy(() -> new WeightedMovingAverage(List.of(0.5, 0.4)))
				.isInstanceOf(InvalidForecastParameterException.class)
				.hasMessageContaining("sumar exactamente 1");
	}

	@Test
	void weightsMustBeGreaterThanZero() {
		assertThatThrownBy(() -> new WeightedMovingAverage(List.of(1.0, 0.0)))
				.isInstanceOf(InvalidForecastParameterException.class);
	}

	@Test
	void decimalWeightsThatSumOneAreAccepted() {
		assertThat(new WeightedMovingAverage(List.of(0.3, 0.2, 0.2, 0.2, 0.1)).weights()).hasSize(5);
	}

	@Test
	void firstWeightAppliesToTheMostRecentValue() {
		Forecast forecast = new WeightedMovingAverage(List.of(0.75, 0.25)).forecast(List.of(100.0, 200.0));

		assertThat(forecast.next()).isEqualTo(0.75 * 200 + 0.25 * 100);
	}

	@ParameterizedTest
	@ValueSource(doubles = { 0, 1, -0.1, 1.5, Double.NaN })
	void smoothingFactorsOutsideTheOpenIntervalAreRejected(double value) {
		assertThatThrownBy(() -> new ExponentialSmoothing(value)).isInstanceOf(InvalidForecastParameterException.class);
		assertThatThrownBy(() -> new TrendAdjustedExponentialSmoothing(0.5, value))
				.isInstanceOf(InvalidForecastParameterException.class)
				.extracting("field").isEqualTo("beta");
	}

	@Test
	void smoothingStartsWithTheFirstValueAndForecastsFromPeriodTwo() {
		Forecast simple = new ExponentialSmoothing(0.5).forecast(SERIES);
		Forecast holt = new TrendAdjustedExponentialSmoothing(0.5, 0.5).forecast(SERIES);

		assertThat(simple.firstPeriod()).isEqualTo(1);
		assertThat(simple.at(1)).isEqualTo(100.0);
		assertThat(holt.firstPeriod()).isEqualTo(1);
		assertThat(holt.at(1)).isEqualTo(100.0);
	}

	@Test
	void singleValueSeriesForecastsThatValue() {
		assertThat(new ExponentialSmoothing(0.3).forecast(List.of(42.0)).next()).isEqualTo(42.0);
		assertThat(new TrendAdjustedExponentialSmoothing(0.3, 0.2).forecast(List.of(42.0)).next()).isEqualTo(42.0);
	}

	@Test
	void seriesMustHaveNonNegativeValues() {
		assertThatThrownBy(() -> new ExponentialSmoothing(0.5).forecast(List.of()))
				.isInstanceOf(InvalidForecastParameterException.class);
		assertThatThrownBy(() -> new ExponentialSmoothing(0.5).forecast(List.of(10.0, -1.0)))
				.isInstanceOf(InvalidForecastParameterException.class);
		assertThatThrownBy(() -> new ExponentialSmoothing(0.5).forecast(Arrays.asList(10.0, null)))
				.isInstanceOf(InvalidForecastParameterException.class);
	}
}
