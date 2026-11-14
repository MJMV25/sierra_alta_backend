package co.sierraalta.production.forecast.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Pattern;

import co.sierraalta.production.auth.application.UserDirectory;
import co.sierraalta.production.forecast.domain.Forecast;
import co.sierraalta.production.forecast.domain.ForecastExecution;
import co.sierraalta.production.forecast.domain.InputValue;
import co.sierraalta.production.forecast.domain.InvalidForecastParameterException;
import co.sierraalta.production.forecast.domain.MethodCode;
import co.sierraalta.production.forecast.domain.MethodRun;
import co.sierraalta.production.forecast.domain.MetricCode;
import co.sierraalta.production.forecast.domain.MetricValue;
import co.sierraalta.production.forecast.domain.ResultPoint;
import co.sierraalta.production.forecast.domain.ResultType;
import co.sierraalta.production.forecast.infrastructure.persistence.ForecastExecutionRepository;
import co.sierraalta.production.history.application.HistoryService;
import co.sierraalta.production.history.application.ObservationValue;
import co.sierraalta.production.history.application.SeriesObservations;
import co.sierraalta.production.history.application.SeriesReference;
import co.sierraalta.production.history.domain.Periodicity;
import co.sierraalta.production.indicators.application.ExecutionComparison;
import co.sierraalta.production.indicators.domain.ErrorIndicators;
import co.sierraalta.production.indicators.domain.PeriodError;
import co.sierraalta.production.shared.Decimals;
import co.sierraalta.production.shared.error.BusinessRuleException;
import co.sierraalta.production.shared.error.InvalidFieldException;
import co.sierraalta.production.shared.error.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade del pronóstico (ADR 04): lee la serie con el servicio público de {@code history}, ejecuta los
 * métodos y calcula MAD y MAPE con {@code indicators} en la ventana común (ADR 07). Guardar deja la
 * ejecución, la instantánea de entradas, los resultados y las métricas en una sola transacción; la vista
 * previa hace los mismos cálculos sin guardar nada (ADR 13).
 *
 * <p>Cada operación exige su permiso aunque la ruta ya lo verifique en {@code SecurityConfig}.
 */
@Service
@Transactional
public class ForecastService {

	/** Cambia cuando cambie una fórmula o regla de cálculo; queda guardada en cada ejecución. */
	static final String CALCULATION_VERSION = "1.0";

	private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

	/** Decimales con que se guardan pronósticos e indicadores; la comparación con el Excel usa 6. */
	private static final int STORED_SCALE = 10;

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final ForecastExecutionRepository executions;
	private final HistoryService history;
	private final UserDirectory users;

	public ForecastService(ForecastExecutionRepository executions, HistoryService history, UserDirectory users) {
		this.executions = executions;
		this.history = history;
		this.users = users;
	}

	/**
	 * Ejecuta y guarda los métodos pedidos. Si el mismo usuario reenvía una {@code idempotencyKey} ya usada,
	 * devuelve la ejecución original sin calcular de nuevo (doble clic, reintento por red).
	 */
	@PreAuthorize("hasAuthority('forecast:execute')")
	public ExecutionOutcome execute(ForecastCommand command, Long actorId, String idempotencyKey) {
		if (idempotencyKey != null && !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
			throw new InvalidFieldException("Idempotency-Key",
					"Use de 1 a 64 letras, números, guiones o guiones bajos.");
		}
		if (idempotencyKey != null) {
			var previous = executions.findByUserIdAndIdempotencyKey(actorId, idempotencyKey);
			if (previous.isPresent()) {
				return new ExecutionOutcome(detail(previous.get()), false);
			}
		}
		requireDistinctMethods(command.methods());

		SeriesObservations series = history.observations(command.seriesId());
		List<ObservationValue> observed = inRange(requireData(series.observations()), command.from(), command.to());
		Calculation calculation = calculate(command.methods(), observed);
		List<InputValue> inputs = observed.stream().map(o -> new InputValue(o.period(), o.value())).toList();
		ForecastExecution saved = executions.save(new ForecastExecution(command.seriesId(), actorId,
				CALCULATION_VERSION, series.dataVersion(), calculation.evaluationStart(), idempotencyKey, inputs,
				calculation.runs()));
		return new ExecutionOutcome(detail(saved), true);
	}

	/**
	 * Calcula sin guardar, con los datos guardados o con los que trae el pedido (hoja con cambios sin guardar,
	 * o una prueba sobre los datos de ejemplo). Lo usan todos los que consultan pronósticos.
	 */
	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('forecast:read')")
	public ForecastPreview preview(PreviewCommand command) {
		ForecastCommand forecast = command.forecast();
		requireDistinctMethods(forecast.methods());
		SeriesObservations series = history.observations(forecast.seriesId());
		boolean draft = command.actuals() != null;
		List<ObservationValue> all = draft ? draftValues(command.actuals()) : series.observations();
		List<ObservationValue> observed = inRange(requireData(all), forecast.from(), forecast.to());
		Calculation calculation = calculate(forecast.methods(), observed);
		List<ForecastDetail.Actual> actuals = observed.stream()
				.map(o -> new ForecastDetail.Actual(o.period(), Decimals.plain(o.value())))
				.toList();
		YearMonth lastPeriod = observed.getLast().period();
		return new ForecastPreview(series.seriesId(), series.productCode(), series.productName(), series.measure(),
				series.periodicity(), series.unit(), draft ? null : series.dataVersion(), draft,
				observed.getFirst().period(), lastPeriod, calculation.evaluationStart(), actuals,
				calculation.runs().stream().map(ForecastService::methodResult).toList(),
				ExecutionComparison.of(ForecastIndicatorsSource.stored(null, series.seriesId(),
						calculation.evaluationStart(), lastPeriod, calculation.runs())));
	}


	/** Últimas 50 ejecuciones, de la más reciente a la más antigua; opcionalmente de una sola serie. */
	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('forecast:read')")
	public List<ForecastSummary> list(Long seriesId) {
		List<ForecastExecution> found = seriesId == null ? executions.findTop50ByOrderByExecutedAtDescIdDesc()
				: executions.findTop50BySeriesIdOrderByExecutedAtDescIdDesc(seriesId);
		Map<Long, SeriesReference> references = history.seriesReferences(
				found.stream().map(ForecastExecution::getSeriesId).distinct().toList());
		return found.stream()
				.map(e -> new ForecastSummary(e.getId(), e.getSeriesId(), productName(references.get(e.getSeriesId())),
						periodicity(references.get(e.getSeriesId())),
						e.getExecutedAt(), e.getUserId(), e.getDataVersion(), e.getEvaluationStart(),
						e.getMethods().stream().map(MethodRun::getMethod).toList()))
				.toList();
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('forecast:read')")
	public ForecastDetail get(Long executionId) {
		return detail(executions.findById(executionId).orElseThrow(
				() -> new ResourceNotFoundException("No existe la ejecución con id " + executionId + ".")));
	}

	/**
	 * Meses de {@code from} a {@code to}, ambos incluidos; un extremo nulo toma el primer o el último mes
	 * cargado. Un extremo fuera de la serie es un error de ese campo: no se recorta en silencio.
	 */
	private static List<ObservationValue> inRange(List<ObservationValue> all, YearMonth from, YearMonth to) {
		YearMonth first = all.getFirst().period();
		YearMonth last = all.getLast().period();
		YearMonth start = from == null ? first : from;
		YearMonth end = to == null ? last : to;
		if (start.isBefore(first) || start.isAfter(last)) {
			throw new InvalidFieldException("from", "La serie tiene datos de " + first + " a " + last + ".");
		}
		if (end.isBefore(first) || end.isAfter(last)) {
			throw new InvalidFieldException("to", "La serie tiene datos de " + first + " a " + last + ".");
		}
		if (end.isBefore(start)) {
			throw new InvalidFieldException("to", "El mes final no puede ser anterior al inicial.");
		}
		return all.stream().filter(o -> !o.period().isBefore(start) && !o.period().isAfter(end)).toList();
	}

	/** Métodos calculados sobre los mismos meses, con el primer periodo de la ventana común. */
	private record Calculation(List<MethodRun> runs, YearMonth evaluationStart) {
	}

	private static Calculation calculate(List<MethodSpec> methods, List<ObservationValue> observed) {
		List<Double> actuals = observed.stream().map(o -> o.value().doubleValue()).toList();
		YearMonth firstMonth = observed.getFirst().period();
		List<Forecast> forecasts = new ArrayList<>();
		for (int i = 0; i < methods.size(); i++) {
			forecasts.add(run(methods.get(i), i, actuals));
		}
		int start = Forecast.commonEvaluationStart(forecasts);
		List<MethodRun> runs = new ArrayList<>();
		for (int i = 0; i < forecasts.size(); i++) {
			runs.add(methodRun(methods.get(i), forecasts.get(i), observed, firstMonth, start));
		}
		return new Calculation(runs, start < actuals.size() ? firstMonth.plusMonths(start) : null);
	}

	private static List<ObservationValue> requireData(List<ObservationValue> observations) {
		if (observations.isEmpty()) {
			throw new BusinessRuleException(
					"La serie no tiene datos cargados; registre sus meses en la hoja antes de pronosticar.");
		}
		return observations;
	}

	/** Datos enviados para la vista previa: meses seguidos y sin repetir, como una serie guardada. */
	private static List<ObservationValue> draftValues(List<PreviewCommand.Actual> actuals) {
		List<PreviewCommand.Actual> sorted = actuals.stream()
				.sorted(Comparator.comparing(PreviewCommand.Actual::period))
				.toList();
		for (int i = 1; i < sorted.size(); i++) {
			YearMonth expected = sorted.get(i - 1).period().plusMonths(1);
			if (!sorted.get(i).period().equals(expected)) {
				throw new InvalidFieldException("actuals", "Los datos deben ser meses seguidos y sin repetir; falta o"
						+ " sobra " + expected + ".");
			}
		}
		return sorted.stream().map(a -> new ObservationValue(a.period(), a.value())).toList();
	}

	private static void requireDistinctMethods(List<MethodSpec> methods) {
		Set<MethodCode> seen = EnumSet.noneOf(MethodCode.class);
		for (int i = 0; i < methods.size(); i++) {
			if (!seen.add(methods.get(i).method())) {
				throw new InvalidFieldException("methods[" + i + "].method", "Este método ya está en la lista.");
			}
		}
	}

	/** Ejecuta un método; un parámetro inválido se informa con su posición, p. ej. {@code methods[2].alpha}. */
	private static Forecast run(MethodSpec spec, int index, List<Double> actuals) {
		try {
			return spec.toAlgorithm().forecast(actuals);
		}
		catch (InvalidForecastParameterException ex) {
			if ("series".equals(ex.field())) {
				throw new BusinessRuleException(ex.getMessage());
			}
			throw new InvalidFieldException("methods[" + index + "]." + ex.field(), ex.getMessage());
		}
	}

	private static MethodRun methodRun(MethodSpec spec, Forecast forecast, List<ObservationValue> observed,
			YearMonth firstMonth, int evaluationStart) {
		List<ResultPoint> results = new ArrayList<>();
		for (int period = forecast.firstPeriod(); period < observed.size(); period++) {
			results.add(ResultPoint.evaluation(firstMonth.plusMonths(period), stored(forecast.at(period)),
					observed.get(period).value()));
		}
		results.add(ResultPoint.estimation(firstMonth.plusMonths(observed.size()), stored(forecast.next())));

		List<Double> actuals = observed.stream().map(o -> o.value().doubleValue()).toList();
		ErrorIndicators indicators = ErrorIndicators.of(forecast.pairs(actuals, evaluationStart));
		List<MetricValue> metrics = List.of(
				new MetricValue(MetricCode.MAD, stored(indicators.mad()), indicators.pairsUsed(), 0),
				new MetricValue(MetricCode.MAPE, stored(indicators.mape()),
						indicators.pairsUsed() - indicators.mapeExcluded(), indicators.mapeExcluded()));
		return new MethodRun(spec.method(), spec.storedPeriods(), spec.storedWeights(), spec.storedAlpha(),
				spec.storedBeta(), spec.storedSeasonLength(), results, metrics);
	}

	private ForecastDetail detail(ForecastExecution execution) {
		Long seriesId = execution.getSeriesId();
		SeriesReference series = history.seriesReferences(List.of(seriesId)).get(seriesId);
		List<ForecastDetail.Actual> actuals = execution.getInputs().stream()
				.map(input -> new ForecastDetail.Actual(input.getPeriod(), Decimals.plain(input.getValue())))
				.toList();
		String executedByUsername = users.usernames(List.of(execution.getUserId())).get(execution.getUserId());
		return new ForecastDetail(execution.getId(), execution.getSeriesId(),
				series == null ? null : series.productCode(), productName(series),
				series == null ? null : series.measure(), periodicity(series), series == null ? null : series.unit(),
				execution.getDataVersion(), execution.getExecutedAt(), execution.getUserId(), executedByUsername,
				actuals.isEmpty() ? null : actuals.getFirst().period(),
				actuals.isEmpty() ? null : actuals.getLast().period(),
				execution.getCalculationVersion(), execution.getEvaluationStart(), actuals,
				execution.getMethods().stream().map(ForecastService::methodResult).toList());
	}

	private static ForecastDetail.MethodResult methodResult(MethodRun run) {
		List<ForecastDetail.Point> fitted = run.getResults().stream()
				.filter(point -> point.getType() == ResultType.EVALUACION)
				.map(ForecastService::point)
				.toList();
		ForecastDetail.Point next = run.getResults().stream()
				.filter(point -> point.getType() == ResultType.ESTIMACION)
				.map(ForecastService::point)
				.findFirst().orElse(null);
		MetricValue mad = run.metric(MetricCode.MAD).orElseThrow();
		MetricValue mape = run.metric(MetricCode.MAPE).orElseThrow();
		return new ForecastDetail.MethodResult(run.getMethod(), run.getPeriods(),
				run.getWeights().stream().map(Decimals::plain).toList(), Decimals.plain(run.getAlpha()),
				Decimals.plain(run.getBeta()), run.getSeasonLength(), fitted, next, Decimals.plain(mad.getValue()),
				mape.getValue() == null ? null : Decimals.plain(mape.getValue().multiply(HUNDRED)),
				mad.getPairsUsed(), mape.getPairsExcluded());
	}

	private static ForecastDetail.Point point(ResultPoint point) {
		PeriodError error = PeriodError.of(point.getActual(), point.getForecast());
		return new ForecastDetail.Point(point.getPeriod(), Decimals.plain(point.getForecast()),
				Decimals.plain(point.getActual()), error == null ? null : Decimals.plain(error.error()),
				error == null ? null : Decimals.plain(error.absolutePercent()));
	}

	private static String productName(SeriesReference series) {
		return series == null ? null : series.productName();
	}

	private static Periodicity periodicity(SeriesReference series) {
		return series == null ? null : series.periodicity();
	}

	private static BigDecimal stored(double value) {
		return BigDecimal.valueOf(value).setScale(STORED_SCALE, RoundingMode.HALF_UP);
	}

	private static BigDecimal stored(OptionalDouble value) {
		return value.isPresent() ? stored(value.getAsDouble()) : null;
	}
}
