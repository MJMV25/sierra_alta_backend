package co.sierraalta.production.history.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import co.sierraalta.production.shared.error.BusinessRuleException;
import org.junit.jupiter.api.Test;

/** La hoja se puede corregir, ampliar y recortar por el final, pero nunca queda con huecos (ADR 07 y 13). */
class MonthlyEditRulesTest {

	private static final YearMonth CURRENT = YearMonth.of(2026, 10);

	private static final Map<YearMonth, BigDecimal> LOADED = Map.of(
			YearMonth.of(2026, 7), BigDecimal.valueOf(10),
			YearMonth.of(2026, 8), BigDecimal.valueOf(20),
			YearMonth.of(2026, 9), BigDecimal.valueOf(30));

	@Test
	void firstLoadOfConsecutiveMonthsInAnyOrderAddsThemSorted() {
		List<PlannedChange> planned = MonthlyEditRules.plan(Map.of(),
				List.of(set(2022, 2, 21), set(2022, 1, 0), set(2022, 3, 7)), CURRENT);

		assertThat(planned).extracting(PlannedChange::period)
				.containsExactly(YearMonth.of(2022, 1), YearMonth.of(2022, 2), YearMonth.of(2022, 3));
		assertThat(planned).allMatch(PlannedChange::adds);
	}

	@Test
	void correctingAMonthKeepsTheOldValueForTheHistory() {
		List<PlannedChange> planned = MonthlyEditRules.plan(LOADED, List.of(set(2026, 8, 25)), CURRENT);

		assertThat(planned).containsExactly(
				new PlannedChange(YearMonth.of(2026, 8), BigDecimal.valueOf(20), BigDecimal.valueOf(25)));
	}

	@Test
	void sameValueIsNotAChange() {
		assertThat(MonthlyEditRules.plan(LOADED, List.of(set(2026, 8, 20)), CURRENT)).isEmpty();
		assertThat(MonthlyEditRules.plan(LOADED, List.of(new ObservationChange(YearMonth.of(2026, 8),
				new BigDecimal("20.0000"))), CURRENT)).isEmpty();
	}

	@Test
	void nextMonthAndEarlierHistoryCanBeAdded() {
		assertThat(MonthlyEditRules.plan(LOADED, List.of(set(2026, 10, 1)), CURRENT)).hasSize(1);
		assertThat(MonthlyEditRules.plan(LOADED, List.of(set(2026, 6, 1)), CURRENT)).hasSize(1);
	}

	@Test
	void lastMonthsCanBeRemoved() {
		List<PlannedChange> planned = MonthlyEditRules.plan(LOADED,
				List.of(remove(2026, 9), remove(2026, 8)), CURRENT);

		assertThat(planned).allMatch(PlannedChange::removes);
		assertThat(planned).extracting(PlannedChange::previous)
				.containsExactly(BigDecimal.valueOf(20), BigDecimal.valueOf(30));
	}

	@Test
	void aMonthWithLaterMonthsCannotBeRemoved() {
		assertThatThrownBy(() -> MonthlyEditRules.plan(LOADED, List.of(remove(2026, 8)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("últimos meses")
				.hasMessageContaining("2026-08");
		assertThatThrownBy(() -> MonthlyEditRules.plan(LOADED, List.of(remove(2026, 7)), CURRENT))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void removingAMonthWithoutDataIsRejected() {
		assertThatThrownBy(() -> MonthlyEditRules.plan(LOADED, List.of(remove(2026, 10)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("No hay datos que borrar en 2026-10");
	}

	@Test
	void leavingAGapIsRejectedNamingTheMissingMonths() {
		assertThatThrownBy(() -> MonthlyEditRules.plan(LOADED, List.of(set(2026, 4, 1)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("2026-05, 2026-06");
	}

	@Test
	void repeatedMonthIsRejected() {
		assertThatThrownBy(() -> MonthlyEditRules.plan(LOADED, List.of(set(2026, 10, 1), set(2026, 10, 2)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("repite");
	}

	@Test
	void futureMonthIsRejected() {
		assertThatThrownBy(() -> MonthlyEditRules.plan(LOADED, List.of(set(2026, 11, 1)), CURRENT))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("posteriores");
	}

	@Test
	void longListsOfMonthsAreTruncated() {
		assertThatThrownBy(() -> MonthlyEditRules.plan(LOADED, List.of(set(2020, 1, 1)), CURRENT))
				.hasMessageContaining("y 65 más");
	}

	private static ObservationChange set(int year, int month, int value) {
		return new ObservationChange(YearMonth.of(year, month), BigDecimal.valueOf(value));
	}

	private static ObservationChange remove(int year, int month) {
		return new ObservationChange(YearMonth.of(year, month), null);
	}
}
