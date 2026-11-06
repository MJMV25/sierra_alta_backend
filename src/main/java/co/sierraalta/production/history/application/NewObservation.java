package co.sierraalta.production.history.application;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Mes y cantidad que se quieren agregar a una serie. */
public record NewObservation(YearMonth period, BigDecimal value) {
}
