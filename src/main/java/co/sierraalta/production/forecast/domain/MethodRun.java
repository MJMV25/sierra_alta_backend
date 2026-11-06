package co.sierraalta.production.forecast.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Un método dentro de una ejecución: sus parámetros, sus pronósticos y sus indicadores. */
@Entity
@Immutable
@Table(name = "metodo_ejecutado")
public class MethodRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ejecucion_id", nullable = false)
	private ForecastExecution execution;

	@Enumerated(EnumType.STRING)
	@Column(name = "metodo", nullable = false, length = 40)
	private MethodCode method;

	/** n de los promedios móviles; en el ponderado, la cantidad de pesos. */
	@Column(name = "periodos")
	private Integer periods;

	@Column(name = "alfa", precision = 11, scale = 10)
	private BigDecimal alpha;

	@Column(name = "beta", precision = 11, scale = 10)
	private BigDecimal beta;

	/** Posición 0 (K0) = peso del dato más reciente. */
	@ElementCollection
	@CollectionTable(name = "peso", joinColumns = @JoinColumn(name = "metodo_ejecutado_id"))
	@OrderColumn(name = "posicion")
	@Column(name = "valor", nullable = false, precision = 11, scale = 10)
	private List<BigDecimal> weights = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "resultado", joinColumns = @JoinColumn(name = "metodo_ejecutado_id"))
	@OrderBy("period")
	private List<ResultPoint> results = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "metrica", joinColumns = @JoinColumn(name = "metodo_ejecutado_id"))
	private List<MetricValue> metrics = new ArrayList<>();

	protected MethodRun() {
		// Requerido por JPA.
	}

	public MethodRun(MethodCode method, Integer periods, List<BigDecimal> weights, BigDecimal alpha, BigDecimal beta,
			List<ResultPoint> results, List<MetricValue> metrics) {
		this.method = method;
		this.periods = periods;
		this.weights = new ArrayList<>(weights);
		this.alpha = alpha;
		this.beta = beta;
		this.results = new ArrayList<>(results);
		this.metrics = new ArrayList<>(metrics);
	}

	void attachTo(ForecastExecution owner) {
		this.execution = owner;
	}

	public Optional<MetricValue> metric(MetricCode code) {
		return metrics.stream().filter(metric -> metric.getCode() == code).findFirst();
	}

	public MethodCode getMethod() {
		return method;
	}

	public Integer getPeriods() {
		return periods;
	}

	public List<BigDecimal> getWeights() {
		return List.copyOf(weights);
	}

	public BigDecimal getAlpha() {
		return alpha;
	}

	public BigDecimal getBeta() {
		return beta;
	}

	public List<ResultPoint> getResults() {
		return List.copyOf(results);
	}
}
