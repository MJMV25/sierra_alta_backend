package co.sierraalta.production.history.application;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Dato real de un mes. El valor se entrega sin ceros decimales sobrantes (120 en lugar de 120.0000). */
public record ObservationValue(YearMonth period, BigDecimal value) {

	static ObservationValue of(YearMonth period, BigDecimal value) {
		BigDecimal plain = value.stripTrailingZeros();
		return new ObservationValue(period, plain.scale() < 0 ? plain.setScale(0) : plain);
	}
}
