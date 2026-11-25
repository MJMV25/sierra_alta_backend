package co.sierraalta.production.forecast.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Estacional multiplicativo, idéntico a las hojas "CASO 1/2 ESTACIONAL MULTIPLI." del Excel de la entrega 2
 * (ADR 12). La serie se parte en ciclos de L periodos desde el primer dato:
 * <ol>
 * <li>promedio de cada ciclo completo c: {@code PROMEDIO(ciclo c)};</li>
 * <li>índice de la posición p: promedio, sobre los ciclos completos, de {@code D(c, p) / promedio(c)};</li>
 * <li>pronóstico de un periodo de un ciclo completo: {@code REDONDEAR(promedio(c) × índice(p); 0)};</li>
 * <li>ciclo proyectado: promedio = (total del último ciclo completo + promedio de los incrementos entre
 * totales consecutivos) / L, y su pronóstico {@code REDONDEAR(promedio proyectado × índice(p); 0)}.</li>
 * </ol>
 * Como en el Excel, cada ciclo completo se pronostica con su propio promedio (ajuste dentro de la muestra) y
 * los valores se redondean a entero. Da un pronóstico desde t = 1, así que no recorta la ventana común.
 * Los periodos de un ciclo incompleto al final, y el periodo siguiente, toman el ciclo proyectado.
 *
 * @param seasonLength L, periodos por ciclo (5 en Spokane, 4 trimestres en ACME, 12 meses en una serie mensual)
 */
public record SeasonalMultiplicative(int seasonLength) implements ForecastMethod {

	/** Con menos de 2 ciclos completos no hay incremento entre ciclos para proyectar el siguiente. */
	static final int MIN_CYCLES = 2;

	/** Excel calcula con 15 cifras significativas. */
	private static final MathContext EXCEL_PRECISION = new MathContext(15, RoundingMode.HALF_UP);

	public SeasonalMultiplicative {
		if (seasonLength < 2) {
			throw new InvalidForecastParameterException("seasonLength", "Debe ser un entero mayor o igual a 2.");
		}
	}

	@Override
	public Forecast forecast(List<Double> actuals) {
		ForecastMethod.requireValidSeries(actuals);
		int cycles = actuals.size() / seasonLength;
		if (cycles < MIN_CYCLES) {
			throw new InvalidForecastParameterException("seasonLength", "Se necesitan al menos " + MIN_CYCLES
					+ " ciclos completos (" + MIN_CYCLES * seasonLength + " datos) y la serie tiene "
					+ actuals.size() + ".");
		}
		double[] totals = new double[cycles];
		double[] averages = new double[cycles];
		for (int c = 0; c < cycles; c++) {
			for (int p = 0; p < seasonLength; p++) {
				totals[c] += actuals.get(c * seasonLength + p);
			}
			averages[c] = totals[c] / seasonLength;
		}
		double[] indices = indices(actuals, cycles, averages);
		double projectedAverage = (totals[cycles - 1] + averageIncrement(totals)) / seasonLength;

		List<Double> fitted = new ArrayList<>();
		for (int period = 0; period < actuals.size(); period++) {
			int cycle = period / seasonLength;
			double average = cycle < cycles ? averages[cycle] : projectedAverage;
			fitted.add(excelRound(average * indices[period % seasonLength]));
		}
		double next = excelRound(projectedAverage * indices[actuals.size() % seasonLength]);
		return new Forecast(0, fitted, next);
	}

	/**
	 * Índice por posición. Un ciclo con promedio 0 no tiene razón definida: se informa en lugar de dividir
	 * por cero (el Excel mostraría #¡DIV/0!).
	 */
	private double[] indices(List<Double> actuals, int cycles, double[] averages) {
		double[] indices = new double[seasonLength];
		for (int c = 0; c < cycles; c++) {
			if (averages[c] == 0) {
				throw new InvalidForecastParameterException("series", "El ciclo " + (c + 1)
						+ " no tiene producción y el método estacional no puede calcular sus índices.");
			}
		}
		for (int p = 0; p < seasonLength; p++) {
			double sum = 0;
			for (int c = 0; c < cycles; c++) {
				sum += actuals.get(c * seasonLength + p) / averages[c];
			}
			indices[p] = sum / cycles;
		}
		return indices;
	}

	/** {@code PROMEDIO} de las diferencias entre totales consecutivos ("incremento prom" del Excel). */
	private static double averageIncrement(double[] totals) {
		double sum = 0;
		for (int c = 1; c < totals.length; c++) {
			sum += totals[c] - totals[c - 1];
		}
		return sum / (totals.length - 1);
	}

	/**
	 * {@code REDONDEAR(x; 0)}: mitades lejos de cero. Como Excel, primero se lleva el valor a 15 cifras
	 * significativas, para que un 282,5 calculado como 282,49999999999997 también suba a 283.
	 */
	static double excelRound(double value) {
		return new BigDecimal(value).round(EXCEL_PRECISION).setScale(0, RoundingMode.HALF_UP).doubleValue();
	}
}
