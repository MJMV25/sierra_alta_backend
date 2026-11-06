package co.sierraalta.production.auth.domain;

import static co.sierraalta.production.auth.domain.Permission.FORECAST_EXECUTE;
import static co.sierraalta.production.auth.domain.Permission.FORECAST_EXPORT;
import static co.sierraalta.production.auth.domain.Permission.FORECAST_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_READ;
import static co.sierraalta.production.auth.domain.Permission.HISTORY_WRITE;
import static co.sierraalta.production.auth.domain.Permission.INDICATORS_READ;
import static co.sierraalta.production.auth.domain.Permission.USERS_MANAGE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Fija el alcance de cada rol (tabla 2 del documento de arquitectura, ajustada el 2026-11-05; ADR 05). */
class RoleCodeTest {

	@Test
	void adminHasEveryPermission() {
		assertThat(RoleCode.ADMIN.permissions()).containsExactlyInAnyOrder(Permission.values());
	}

	@Test
	void plannerReadsAndExports() {
		assertThat(RoleCode.PLANNER.permissions())
				.containsExactlyInAnyOrder(HISTORY_READ, FORECAST_READ, INDICATORS_READ, FORECAST_EXPORT);
	}

	@Test
	void onlyPlannerAndAdminExport() {
		for (RoleCode role : RoleCode.values()) {
			assertThat(role.grants(FORECAST_EXPORT)).as(role.name())
					.isEqualTo(role == RoleCode.PLANNER || role == RoleCode.ADMIN);
		}
	}

	@Test
	void analystLoadsHistoryButDoesNotExecuteForecasts() {
		assertThat(RoleCode.ANALYST.permissions())
				.containsExactlyInAnyOrder(HISTORY_READ, HISTORY_WRITE, FORECAST_READ, INDICATORS_READ);
	}

	@Test
	void onlyAnalystAndAdminWriteHistory() {
		for (RoleCode role : RoleCode.values()) {
			assertThat(role.grants(HISTORY_WRITE)).as(role.name())
					.isEqualTo(role == RoleCode.ANALYST || role == RoleCode.ADMIN);
		}
	}

	@Test
	void onlyAdminExecutesForecasts() {
		for (RoleCode role : RoleCode.values()) {
			assertThat(role.grants(FORECAST_EXECUTE)).as(role.name()).isEqualTo(role == RoleCode.ADMIN);
		}
	}

	@Test
	void onlyAdminManagesAccounts() {
		for (RoleCode role : RoleCode.values()) {
			assertThat(role.grants(USERS_MANAGE)).as(role.name()).isEqualTo(role == RoleCode.ADMIN);
		}
	}
}
