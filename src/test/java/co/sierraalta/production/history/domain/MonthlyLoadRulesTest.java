package co.sierraalta.production.history.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.shared.error.BusinessRuleException;
import co.sierraalta.production.shared.error.ConflictException;
import org.junit.jupiter.api.Test;

/** Una serie mensual nunca queda con huecos ni meses repetidos (ADR 07). */
class MonthlyLoadRulesTest {

	private static final YearMonth CURRENT = YearMonth.of(2026, 10);

	private static final List<YearMonth> LOADED = List.of(YearMonth.of(2026, 7), YearMonth.of(2026, 8),
			YearMonth.of(2026, 9));

	@Test
	void firstLoadOfConsecutiveMonthsInAnyOrderIsAccepted() {
		assertThatCode(() -> MonthlyLoadRules.check(List.of(),
				List.of(YearMonth.of(2022, 2), YearMonth.of(2022, 1), YearMonth.of(2022, 3)), CURRENT))
				.doesNotThrowAnyException();
	}

	@Test
	void nextMonthAndEarlierHistoryAreAccepted() {
		assertThatCode(() -> MonthlyLoadRules.check(LOADED, List.of(YearMonth.of(2026, 10)), CURRENT))
				.doesNotThrowAnyException();
		assertThatCode(() -> MonthlyLoadRules.check(LOADED, List.of(YearMonth.of(2026, 6)), CURRENT))
				.doesNotThrowAnyException();
	}

	@Test
	void monthAlreadyLoadedIsAConflict() {
		assertThatThrownBy(() -> MonthlyLoadRules.check(LOADED, List.of(YearMonth.of(2026, 9)), CURRENT))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("2026-09");
	}

	@Test
	void leavingAGapIsRejectedNamingTheMissingMonths() {
		assertThatThrownBy(() -> MonthlyLoadRules.check(LOADED, List.of(YearMonth.of(2026, 4)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("2026-05, 2026-06");
	}

	@Test
	void gapInsideTheBatchIsRejected() {
		assertThatThrownBy(() -> MonthlyLoadRules.check(List.of(),
				List.of(YearMonth.of(2022, 1), YearMonth.of(2022, 3)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("2022-02");
	}

	@Test
	void repeatedMonthInTheBatchIsRejected() {
		assertThatThrownBy(() -> MonthlyLoadRules.check(LOADED,
				List.of(YearMonth.of(2026, 10), YearMonth.of(2026, 10)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("repite");
	}

	@Test
	void futureMonthIsRejected() {
		assertThatThrownBy(() -> MonthlyLoadRules.check(LOADED, List.of(YearMonth.of(2026, 11)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("posteriores");
	}

	@Test
	void longListsOfMonthsAreTruncated() {
		assertThatThrownBy(() -> MonthlyLoadRules.check(LOADED, List.of(YearMonth.of(2020, 1)), CURRENT))
				.hasMessageContaining("y 65 más");
	}
}
