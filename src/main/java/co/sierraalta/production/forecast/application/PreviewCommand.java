package co.sierraalta.production.forecast.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Pedido de recálculo sin guardar. {@code actuals} opcional: datos de la hoja que aún no se guardan (o una
 * prueba con los datos de ejemplo); si es nulo se usan los guardados.
 */
public record PreviewCommand(ForecastCommand forecast, List<Actual> actuals) {

	public record Actual(YearMonth period, BigDecimal value) {
	}
}
