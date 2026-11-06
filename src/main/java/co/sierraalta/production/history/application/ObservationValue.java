package co.sierraalta.production.history.application;

import java.math.BigDecimal;
import java.time.YearMonth;

import co.sierraalta.production.shared.Decimals;

/** Dato real de un mes. El valor se entrega sin ceros decimales sobrantes (120 en lugar de 120.0000). */
public record ObservationValue(YearMonth period, BigDecimal value) {

	static ObservationValue of(YearMonth period, BigDecimal value) {
		return new ObservationValue(period, Decimals.plain(value));
	}
}
