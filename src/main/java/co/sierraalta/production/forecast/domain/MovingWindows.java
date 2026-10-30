package co.sierraalta.production.forecast.domain;

/** Regla compartida por los promedios móviles. */
final class MovingWindows {

	private MovingWindows() {
	}

	/**
	 * n no puede superar los datos disponibles. Con n igual al total solo se obtiene el pronóstico del
	 * periodo siguiente y los indicadores quedan "no calculables" (propuesta de la pregunta 13, por
	 * confirmar con Industrial).
	 */
	static void requireEnoughData(int periods, int available) {
		if (periods > available) {
			throw new InvalidForecastParameterException("periods",
					"Se necesitan al menos " + periods + " datos y la serie tiene " + available + ".");
		}
	}
}
