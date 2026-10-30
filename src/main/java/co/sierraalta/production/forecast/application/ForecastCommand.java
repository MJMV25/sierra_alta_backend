package co.sierraalta.production.forecast.application;

import java.util.List;

/** Pedido de ejecución: una serie y de 1 a 4 métodos distintos, que se comparan en una ventana común. */
public record ForecastCommand(Long seriesId, List<MethodSpec> methods) {
}
