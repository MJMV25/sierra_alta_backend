package co.sierraalta.production.indicators.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tabla comparativa de los métodos de una ejecución (ADR 07 y 13).
 *
 * <p>Se señala el mejor por MAD y el mejor por MAPE (pueden ser métodos distintos). Dos métodos empatan si su
 * indicador coincide a 6 decimales, la tolerancia acordada con el Excel; en ese caso ambos quedan marcados.
 * Un método no comparable (el estacional multiplicativo, que se ajusta dentro de la muestra) aparece en la
 * tabla pero no compite por ser el mejor. Sin estado de aceptación: no hay umbral (ADR 13).
 */
public final class MethodComparison {

	/** Decimales con que se decide un empate, igual que la comparación con el Excel (respuesta 6). */
	private static final int TIE_SCALE = 6;

	private MethodComparison() {
	}

	/**
	 * @param methods indicadores de cada método, en el orden en que se ejecutaron
	 * @return una fila por método, en el mismo orden
	 */
	public static List<Row> of(List<MethodIndicators> methods) {
		List<MethodIndicators> competing = methods.stream().filter(MethodIndicators::comparable).toList();
		Optional<BigDecimal> bestMad = best(competing.stream().map(MethodIndicators::mad).toList());
		Optional<BigDecimal> bestMape = best(competing.stream().map(MethodIndicators::mape).toList());
		return methods.stream()
				.map(m -> new Row(m, m.comparable() && ties(m.mad(), bestMad),
						m.comparable() && ties(m.mape(), bestMape)))
				.toList();
	}

	private static Optional<BigDecimal> best(List<BigDecimal> values) {
		return values.stream().filter(Objects::nonNull).map(MethodComparison::rounded).min(Comparator.naturalOrder());
	}

	private static boolean ties(BigDecimal value, Optional<BigDecimal> best) {
		return value != null && best.isPresent() && rounded(value).compareTo(best.get()) == 0;
	}

	private static BigDecimal rounded(BigDecimal value) {
		return value.setScale(TIE_SCALE, RoundingMode.HALF_UP);
	}

	/**
	 * Indicadores de un método. {@code mad} en unidades y {@code mape} como fracción; nulos si no son
	 * calculables. {@code comparable} es falso si el método no compite por ser el mejor.
	 */
	public record MethodIndicators(String method, BigDecimal mad, BigDecimal mape, int pairsUsed, int mapeExcluded,
			boolean comparable) {
	}

	/** Fila de la tabla: los indicadores del método y si es el mejor por cada uno. */
	public record Row(MethodIndicators indicators, boolean bestByMad, boolean bestByMape) {
	}
}
