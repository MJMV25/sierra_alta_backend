package co.sierraalta.production.forecast.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Indicador de un método (tabla {@code metrica}). Valor nulo = no calculable. El MAPE se guarda como
 * fracción, igual que la celda del Excel.
 */
@Embeddable
public class MetricValue {

	@Enumerated(EnumType.STRING)
	@Column(name = "codigo", nullable = false, length = 20)
	private MetricCode code;

	@Column(name = "valor", precision = 24, scale = 10)
	private BigDecimal value;

	@Column(name = "pares_usados", nullable = false)
	private int pairsUsed;

	@Column(name = "pares_excluidos", nullable = false)
	private int pairsExcluded;

	protected MetricValue() {
		// Requerido por JPA.
	}

	public MetricValue(MetricCode code, BigDecimal value, int pairsUsed, int pairsExcluded) {
		this.code = code;
		this.value = value;
		this.pairsUsed = pairsUsed;
		this.pairsExcluded = pairsExcluded;
	}

	public MetricCode getCode() {
		return code;
	}

	public BigDecimal getValue() {
		return value;
	}

	public int getPairsUsed() {
		return pairsUsed;
	}

	public int getPairsExcluded() {
		return pairsExcluded;
	}
}
