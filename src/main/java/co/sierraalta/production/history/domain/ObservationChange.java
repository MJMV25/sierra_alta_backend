package co.sierraalta.production.history.domain;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Lo que se pide para un mes de la hoja: su nuevo valor, o {@code null} para borrarlo. */
public record ObservationChange(YearMonth period, BigDecimal value) {

	public boolean removes() {
		return value == null;
	}
}
