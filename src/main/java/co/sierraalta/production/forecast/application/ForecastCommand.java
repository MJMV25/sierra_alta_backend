package co.sierraalta.production.forecast.application;

import java.time.YearMonth;
import java.util.List;

/**
 * Pedido de ejecución: una serie, el periodo histórico a usar y de 1 a 4 métodos distintos, que se comparan
 * en una ventana común. {@code from} y {@code to} son opcionales: nulos usan el primer y el último mes
 * cargados (requisito funcional 3 de la Entrega 1).
 */
public record ForecastCommand(Long seriesId, YearMonth from, YearMonth to, List<MethodSpec> methods) {

	/** Toda la serie cargada. */
	public ForecastCommand(Long seriesId, List<MethodSpec> methods) {
		this(seriesId, null, null, methods);
	}
}
