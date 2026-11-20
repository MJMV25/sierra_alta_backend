package co.sierraalta.production.indicators.domain;

import java.util.List;
import java.util.OptionalDouble;

/**
 * MAD, MAPE y MSE de un conjunto de pares. MSE = promedio del error al cuadrado.
 *
 * <p>MAD = Σ|E(t)| / n. MAPE = Σ(|E(t)| / D(t)) / n, como fracción (igual que la celda del Excel; la
 * interfaz la muestra ×100). Los pares con real exactamente 0 se excluyen solo del MAPE y se cuentan en
 * {@code mapeExcluded}. Un indicador sin pares para calcular queda vacío ("no calculable"), nunca en cero.
 *
 * @param mad          vacío si no hay pares
 * @param mape         vacío si no hay pares con real distinto de cero
 * @param mse          vacío si no hay pares; incluye los reales cero
 * @param pairsUsed    pares evaluados (todos entran al MAD)
 * @param mapeExcluded pares con real 0, excluidos del MAPE
 */
public record ErrorIndicators(OptionalDouble mad, OptionalDouble mape, OptionalDouble mse, int pairsUsed, int mapeExcluded) {

	public static ErrorIndicators of(List<ForecastPair> pairs) {
		double absoluteErrors = 0;
		double percentageErrors = 0;
		double squaredErrors = 0;
		int mapeExcluded = 0;
		for (ForecastPair pair : pairs) {
			absoluteErrors += pair.absoluteError();
			squaredErrors += pair.absoluteError() * pair.absoluteError();
			if (pair.actual() == 0) {
				mapeExcluded++;
			}
			else {
				percentageErrors += pair.absoluteError() / pair.actual();
			}
		}
		int mapePairs = pairs.size() - mapeExcluded;
		return new ErrorIndicators(
				pairs.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(absoluteErrors / pairs.size()),
				mapePairs == 0 ? OptionalDouble.empty() : OptionalDouble.of(percentageErrors / mapePairs),
				pairs.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(squaredErrors / pairs.size()),
				pairs.size(), mapeExcluded);
	}
}
