package co.sierraalta.production.indicators.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PeriodErrorTest {

	@Test
	void errorIsActualMinusForecastAndPercentIsOverTheActual() {
		PeriodError error = PeriodError.of(new BigDecimal("268"), new BigDecimal("289.8"));

		assertThat(error.error()).isEqualByComparingTo("-21.8");
		assertThat(error.absolutePercent()).isEqualByComparingTo("8.1343283582");
	}

	@Test
	void zeroActualHasNoPercentAndMissingActualHasNoError() {
		assertThat(PeriodError.of(BigDecimal.ZERO, BigDecimal.TEN).absolutePercent()).isNull();
		assertThat(PeriodError.of(null, BigDecimal.TEN)).isNull();
	}
}
