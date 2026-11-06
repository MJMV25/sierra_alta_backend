package co.sierraalta.production.indicators.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tabla comparativa de los métodos de una ejecución (ADR 07).
 *
 * <p>Ni el MAD ni el MAPE mandan (pregunta 22): se señala el mejor por cada indicador, sin un ganador único.
 * Dos métodos empatan si su indicador coincide a 6 decimales, la tolerancia acordada con el Excel; en ese
 * caso ambos quedan marcados como mejores. Cada método cumple si su MAPE es menor que el umbral; igual o
 * mayor no cumple (pregunta 21).
 */
public final class MethodComparison {

	/** Decimales con que se decide un empate, igual que la comparación con el Excel (respuesta 6). */
	private static final int TIE_SCALE = 6;

	private MethodComparison() {
	}

	/**
	 * @param mapeThreshold umbral como fracción (0,10 = 10 %); nulo si la ejecución no tiene criterio
	 * @param methods       indicadores de cada método, en el orden en que se ejecutaron
	 * @return una fila por método, en el mismo orden
	 */
	public static List<Row> of(BigDecimal mapeThreshold, List<MethodIndicators> methods) {
		Optional<BigDecimal> bestMad = best(methods.stream().map(MethodIndicators::mad).toList());
		Optional<BigDecimal> bestMape = best(methods.stream().map(MethodIndicators::mape).toList());
		return methods.stream()
				.map(m -> new Row(m, ties(m.mad(), bestMad), ties(m.mape(), bestMape), status(m.mape(), mapeThreshold)))
				.toList();
	}

	static AcceptanceStatus status(BigDecimal mape, BigDecimal mapeThreshold) {
		if (mapeThreshold == null) {
			return AcceptanceStatus.SIN_CRITERIO;
		}
		if (mape == null) {
			return AcceptanceStatus.SIN_EVALUAR;
		}
		return mape.compareTo(mapeThreshold) < 0 ? AcceptanceStatus.CUMPLE : AcceptanceStatus.NO_CUMPLE;
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
	 * Indicadores guardados de un método. {@code mad} en unidades y {@code mape} como fracción; nulos si no
	 * son calculables.
	 */
	public record MethodIndicators(String method, BigDecimal mad, BigDecimal mape, int pairsUsed, int mapeExcluded) {
	}

	/** Fila de la tabla: los indicadores del método, si es el mejor por cada uno y su estado frente al umbral. */
	public record Row(MethodIndicators indicators, boolean bestByMad, boolean bestByMape, AcceptanceStatus status) {
	}
}
