package co.sierraalta.production.indicators.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Umbral de aceptación del MAPE (ADR 07, pregunta 21). Cada ejecución guarda el umbral vigente al
 * ejecutarse, así que cambiarlo no altera el estado de las ejecuciones anteriores.
 *
 * @param mapeThresholdPercent porcentaje estrictamente entre 0 y 100; 10 si no se configura
 */
@ConfigurationProperties("app.indicators")
public record IndicatorsProperties(BigDecimal mapeThresholdPercent) {

	private static final BigDecimal DEFAULT_PERCENT = BigDecimal.TEN;
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	public IndicatorsProperties {
		mapeThresholdPercent = mapeThresholdPercent == null ? DEFAULT_PERCENT : mapeThresholdPercent;
		if (mapeThresholdPercent.signum() <= 0 || mapeThresholdPercent.compareTo(HUNDRED) >= 0) {
			throw new IllegalArgumentException(
					"app.indicators.mape-threshold-percent debe estar entre 0 y 100 (sin incluirlos).");
		}
	}

	/** El umbral como fracción, en la misma escala en que se guarda el MAPE (0,10 = 10 %). */
	public BigDecimal mapeThreshold() {
		return mapeThresholdPercent.divide(HUNDRED, 10, RoundingMode.HALF_UP);
	}
}
