package co.sierraalta.production.history.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

/**
 * Línea del historial de cambios de una serie. {@code previousValue} nulo: mes agregado; {@code newValue}
 * nulo: mes borrado. {@code changedBy} es nulo si la cuenta ya no existe.
 */
public record ChangeEntry(Long loadId, YearMonth period, BigDecimal previousValue, BigDecimal newValue,
		Instant changedAt, String changedBy) {
}
