package co.sierraalta.production.history.domain;

import java.time.YearMonth;

/**
 * Periodicidad de una serie; ampliarla exige una migración del CHECK de {@code serie}.
 *
 * <ul>
 * <li>{@link #MENSUAL}: datos de la empresa (respuesta 18).</li>
 * <li>{@link #PERIODO}: casos de prueba del Excel (Spokane, ACME), numerados t = 1, 2, 3… Cada t se guarda
 * como un mes ficticio desde {@link #FIRST_TEST_PERIOD} (V7) y la interfaz lo muestra como "t = n". Son
 * series de solo lectura: no admiten cargas.</li>
 * </ul>
 */
public enum Periodicity {
	MENSUAL,
	PERIODO;

	/** Mes con el que se guarda t = 1 en las series {@link #PERIODO}. */
	public static final YearMonth FIRST_TEST_PERIOD = YearMonth.of(2000, 1);
}
