package co.sierraalta.production.indicators.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import co.sierraalta.production.indicators.domain.AcceptanceStatus;

/**
 * Tabla comparativa de una ejecución. Siempre incluye el rango evaluado: con la ventana común, el MAD de un
 * método depende de con cuáles se comparó (ADR 07).
 *
 * @param mapeThresholdPercent umbral aplicado en porcentaje; nulo si la ejecución no tiene criterio
 * @param bestByMad            métodos con el menor MAD (varios si empatan a 6 decimales)
 * @param bestByMape           métodos con el menor MAPE
 */
public record ExecutionComparison(Long executionId, Long seriesId, YearMonth evaluationFrom, YearMonth evaluationTo,
		BigDecimal mapeThresholdPercent, List<String> bestByMad, List<String> bestByMape, List<MethodRow> methods) {

	/**
	 * {@code mad} en unidades y {@code mapePercent} en porcentaje; nulos si no son calculables.
	 * {@code mapeExcluded} periodos con real 0 que no entraron al MAPE.
	 */
	public record MethodRow(String method, BigDecimal mad, BigDecimal mapePercent, int pairsUsed, int mapeExcluded,
			boolean bestByMad, boolean bestByMape, AcceptanceStatus status) {
	}
}
