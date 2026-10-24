package co.sierraalta.production.auth.domain;

import static co.sierraalta.production.auth.domain.Permission.FORECAST_EXECUTE;
import static co.sierraalta.production.auth.domain.Permission.FORECAST_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_WRITE;
import static co.sierraalta.production.auth.domain.Permission.INDICATORS_READ;
import static co.sierraalta.production.auth.domain.Permission.USERS_MANAGE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Fija el alcance de cada rol según la tabla 2 del documento de arquitectura. */
class RoleCodeTest {

	@Test
	void adminOnlyManagesAccounts() {
		assertThat(RoleCode.ADMIN.permissions()).containsExactly(USERS_MANAGE);
	}

	@Test
	void plannerReadsHistoryExecutesForecastsAndReadsIndicators() {
		assertThat(RoleCode.PLANNER.permissions())
				.containsExactlyInAnyOrder(HISTORY_READ, FORECAST_EXECUTE, FORECAST_READ, INDICATORS_READ);
	}

	@Test
	void supervisorOnlyReads() {
		assertThat(RoleCode.SUPERVISOR.permissions())
				.containsExactlyInAnyOrder(HISTORY_READ, FORECAST_READ, INDICATORS_READ);
	}

	@Test
	void analystLoadsHistoryAndExecutesForecasts() {
		assertThat(RoleCode.ANALYST.permissions()).containsExactlyInAnyOrder(HISTORY_READ, HISTORY_WRITE,
				FORECAST_EXECUTE, FORECAST_READ, INDICATORS_READ);
	}

	@Test
	void onlyAnalystCanWriteHistory() {
		for (RoleCode role : RoleCode.values()) {
			assertThat(role.grants(HISTORY_WRITE)).as(role.name()).isEqualTo(role == RoleCode.ANALYST);
		}
	}

	@Test
	void onlyAdminManagesAccounts() {
		for (RoleCode role : RoleCode.values()) {
			assertThat(role.grants(USERS_MANAGE)).as(role.name()).isEqualTo(role == RoleCode.ADMIN);
		}
	}
}
