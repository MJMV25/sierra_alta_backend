package co.sierraalta.production.shared;

import java.math.BigDecimal;

/** Presentación de cantidades decimales en las respuestas JSON. */
public final class Decimals {

	private Decimals() {
	}

	/** Quita ceros decimales sobrantes sin pasar a notación científica: 120.0000 → 120, nunca 1.2E+2. */
	public static BigDecimal plain(BigDecimal value) {
		if (value == null) {
			return null;
		}
		BigDecimal stripped = value.stripTrailingZeros();
		return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
	}
}
