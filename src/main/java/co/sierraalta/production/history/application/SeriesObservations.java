package co.sierraalta.production.history.application;

import java.util.List;

import co.sierraalta.production.history.domain.Measure;
import co.sierraalta.production.history.domain.Periodicity;

/**
 * Serie completa ordenada del mes más antiguo al más reciente, sin huecos. Es lo que recibirá el módulo
 * {@code forecast}; {@code dataVersion} identifica la última carga incluida.
 */
public record SeriesObservations(Long seriesId, String productCode, String productName, Measure measure,
		Periodicity periodicity, String unit, Long dataVersion, List<ObservationValue> observations) {
}
