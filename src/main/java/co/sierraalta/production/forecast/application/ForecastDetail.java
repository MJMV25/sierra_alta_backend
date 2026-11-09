package co.sierraalta.production.forecast.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.forecast.domain.MethodCode;
import co.sierraalta.production.history.domain.Measure;
import co.sierraalta.production.history.domain.Periodicity;

/**
 * Ejecución completa: lo que se muestra en pantalla y se exportará (respuesta 23 de Industrial).
 *
 * @param periodicity     MENSUAL o PERIODO (caso de prueba del Excel, periodos "t = n")
 * @param dataVersion     id de la última carga de la serie al ejecutar
 * @param executedBy         id del usuario que ejecutó
 * @param executedByUsername su nombre de usuario; nulo si la cuenta ya no existe
 * @param periodFrom         primer mes histórico usado (periodo elegido al ejecutar)
 * @param periodTo           último mes histórico usado; el pronóstico siguiente es el mes posterior
 * @param evaluationStart    primer periodo evaluado en todos los métodos; nulo si no hubo ninguno
 * @param actuals            instantánea de los reales usados
 */
public record ForecastDetail(Long id, Long seriesId, String productCode, String productName, Measure measure,
		Periodicity periodicity,
		String unit, Long dataVersion, Instant executedAt, Long executedBy, String executedByUsername,
		YearMonth periodFrom, YearMonth periodTo, String calculationVersion,
		YearMonth evaluationStart, List<Actual> actuals, List<MethodResult> methods) {

	public record Actual(YearMonth period, BigDecimal value) {
	}

	/** {@code actual} es nulo en el pronóstico del periodo siguiente. */
	public record Point(YearMonth period, BigDecimal forecast, BigDecimal actual) {
	}

	/**
	 * Resultado de un método. {@code mad} en unidades y {@code mapePercent} en porcentaje (fracción × 100);
	 * nulos si no son calculables. {@code pairsUsed} periodos evaluados; {@code mapeExcluded} de ellos con
	 * real 0, que no entran al MAPE.
	 */
	public record MethodResult(MethodCode method, Integer periods, List<BigDecimal> weights, BigDecimal alpha,
			BigDecimal beta, List<Point> forecasts, Point nextForecast, BigDecimal mad, BigDecimal mapePercent,
			int pairsUsed, int mapeExcluded) {
	}
}
