package co.sierraalta.production.history.application;

import co.sierraalta.production.history.domain.Measure;
import co.sierraalta.production.history.domain.Periodicity;

/** Identificación de una serie sin sus datos: lo que otros módulos muestran junto a sus resultados. */
public record SeriesReference(Long seriesId, String productCode, String productName, Measure measure,
		Periodicity periodicity, String unit) {
}
