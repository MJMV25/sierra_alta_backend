package co.sierraalta.production.history.application;

import java.util.List;

import co.sierraalta.production.history.domain.ObservationChange;

/**
 * Cambios pedidos para una serie desde la hoja.
 *
 * @param expectedDataVersion versión de datos que tenía la hoja al abrirse; si la serie cambió después, el
 *                            guardado se rechaza para no pisar el trabajo de otro. Nulo para no comprobarla.
 */
public record SeriesEdit(Long seriesId, Long expectedDataVersion, List<ObservationChange> changes) {
}
