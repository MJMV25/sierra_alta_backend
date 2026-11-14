package co.sierraalta.production.history.domain;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Cambio que sí se aplicará a un mes: {@code previous} nulo agrega el mes, {@code next} nulo lo borra y con
 * ambos se corrige.
 */
public record PlannedChange(YearMonth period, BigDecimal previous, BigDecimal next) {

	public boolean adds() {
		return previous == null;
	}

	public boolean removes() {
		return next == null;
	}
}
