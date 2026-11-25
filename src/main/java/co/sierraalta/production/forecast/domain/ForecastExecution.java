package co.sierraalta.production.forecast.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Ejecución de uno o varios métodos sobre una serie (tabla {@code ejecucion}): autor, fecha, versión del
 * cálculo, versión de datos, instantánea de los reales usados y ventana de evaluación común. Inmutable:
 * explica el resultado aunque después se agreguen meses a la serie.
 */
@Entity
@Immutable
@Table(name = "ejecucion")
public class ForecastExecution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Ids de otros módulos: este módulo no mapea las entidades de {@code history} ni de {@code auth}. */
	@Column(name = "serie_id", nullable = false)
	private Long seriesId;

	@Column(name = "usuario_id", nullable = false)
	private Long userId;

	@Column(name = "fecha", nullable = false)
	private Instant executedAt;

	@Column(name = "version_calculo", nullable = false, length = 20)
	private String calculationVersion;

	/** Id de la última carga de la serie al ejecutar. */
	@Column(name = "carga_id", nullable = false)
	private Long dataVersion;

	/** Primer periodo evaluado en todos los métodos; nulo si no hubo ninguno. */
	@Column(name = "evaluacion_desde")
	private LocalDate evaluationStart;

	@Column(name = "clave_idempotencia", length = 64)
	private String idempotencyKey;

	@ElementCollection
	@CollectionTable(name = "entrada", joinColumns = @JoinColumn(name = "ejecucion_id"))
	@OrderBy("period")
	private List<InputValue> inputs = new ArrayList<>();

	@OneToMany(mappedBy = "execution", cascade = CascadeType.PERSIST)
	@OrderBy("id")
	private List<MethodRun> methods = new ArrayList<>();

	protected ForecastExecution() {
		// Requerido por JPA.
	}

	public ForecastExecution(Long seriesId, Long userId, String calculationVersion, Long dataVersion,
			YearMonth evaluationStart, String idempotencyKey, List<InputValue> inputs, List<MethodRun> methods) {
		this.seriesId = seriesId;
		this.userId = userId;
		this.executedAt = Instant.now();
		this.calculationVersion = calculationVersion;
		this.dataVersion = dataVersion;
		this.evaluationStart = evaluationStart == null ? null : evaluationStart.atDay(1);
		this.idempotencyKey = idempotencyKey;
		this.inputs = new ArrayList<>(inputs);
		this.methods = new ArrayList<>(methods);
		this.methods.forEach(method -> method.attachTo(this));
	}

	public Long getId() {
		return id;
	}

	public Long getSeriesId() {
		return seriesId;
	}

	public Long getUserId() {
		return userId;
	}

	public Instant getExecutedAt() {
		return executedAt;
	}

	public String getCalculationVersion() {
		return calculationVersion;
	}

	public Long getDataVersion() {
		return dataVersion;
	}

	public YearMonth getEvaluationStart() {
		return evaluationStart == null ? null : YearMonth.from(evaluationStart);
	}

	public List<InputValue> getInputs() {
		return List.copyOf(inputs);
	}

	public List<MethodRun> getMethods() {
		return List.copyOf(methods);
	}
}
