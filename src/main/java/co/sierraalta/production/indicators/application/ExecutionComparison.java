package co.sierraalta.production.indicators.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.indicators.application.ExecutionIndicatorsSource.StoredIndicators;
import co.sierraalta.production.indicators.domain.MethodComparison;
import co.sierraalta.production.indicators.domain.MethodComparison.Row;
import co.sierraalta.production.shared.Decimals;

/**
 * Tabla comparativa de una ejecución, guardada o recalculada sin guardar. Siempre incluye el rango evaluado:
 * con la ventana común, el MAD de un método depende de con cuáles se comparó (ADR 07).
 *
 * @param executionId nulo si es un recálculo sin guardar
 * @param bestByMad   métodos con el menor MAD (varios si empatan a 6 decimales); nunca uno no comparable
 * @param bestByMape  métodos con el menor MAPE
 */
public record ExecutionComparison(Long executionId, Long seriesId, YearMonth evaluationFrom, YearMonth evaluationTo,
		List<String> bestByMad, List<String> bestByMape, List<MethodRow> methods) {

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	/**
	 * {@code mad} en unidades y {@code mapePercent} en porcentaje; nulos si no son calculables.
	 * {@code mapeExcluded} periodos con real 0 que no entraron al MAPE. {@code comparable} es falso si el
	 * método no compite por ser el mejor (estacional multiplicativo).
	 */
	public record MethodRow(String method, BigDecimal mad, BigDecimal mapePercent, int pairsUsed, int mapeExcluded,
			boolean comparable, boolean bestByMad, boolean bestByMape) {
	}

	public static ExecutionComparison of(StoredIndicators stored) {
		List<Row> rows = MethodComparison.of(stored.methods());
		return new ExecutionComparison(stored.executionId(), stored.seriesId(), stored.evaluationFrom(),
				stored.evaluationTo(),
				rows.stream().filter(Row::bestByMad).map(row -> row.indicators().method()).toList(),
				rows.stream().filter(Row::bestByMape).map(row -> row.indicators().method()).toList(),
				rows.stream().map(ExecutionComparison::methodRow).toList());
	}

	private static MethodRow methodRow(Row row) {
		var m = row.indicators();
		return new MethodRow(m.method(), Decimals.plain(m.mad()), percent(m.mape()),
				m.pairsUsed(), m.mapeExcluded(), m.comparable(), row.bestByMad(), row.bestByMape());
	}

	private static BigDecimal percent(BigDecimal fraction) {
		return fraction == null ? null : Decimals.plain(fraction.multiply(HUNDRED));
	}
}
