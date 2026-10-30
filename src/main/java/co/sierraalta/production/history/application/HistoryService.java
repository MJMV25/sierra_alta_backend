package co.sierraalta.production.history.application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.sierraalta.production.history.domain.DataLoad;
import co.sierraalta.production.history.domain.MonthlyLoadRules;
import co.sierraalta.production.history.domain.Observation;
import co.sierraalta.production.history.domain.Product;
import co.sierraalta.production.history.domain.Series;
import co.sierraalta.production.history.infrastructure.persistence.DataLoadRepository;
import co.sierraalta.production.history.infrastructure.persistence.ObservationRepository;
import co.sierraalta.production.history.infrastructure.persistence.SeriesRepository;
import co.sierraalta.production.history.infrastructure.persistence.SeriesStats;
import co.sierraalta.production.shared.error.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de históricos: consultar referencias y series, y agregar meses a una serie.
 *
 * <p>Es el servicio público del módulo: {@code forecast} leerá las series por aquí, nunca por las tablas.
 * Cada operación exige su permiso aunque la ruta ya lo verifique en {@code SecurityConfig}.
 */
@Service
@Transactional
public class HistoryService {

	/** Zona de la empresa: define cuál es el mes en curso. */
	static final ZoneId COMPANY_ZONE = ZoneId.of("America/Bogota");

	private final SeriesRepository series;
	private final ObservationRepository observations;
	private final DataLoadRepository loads;

	public HistoryService(SeriesRepository series, ObservationRepository observations, DataLoadRepository loads) {
		this.series = series;
		this.observations = observations;
		this.loads = loads;
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('history:read')")
	public List<ProductSummary> products() {
		Map<Long, SeriesStats> stats = observations.summarizeAll().stream()
				.collect(Collectors.toMap(SeriesStats::seriesId, Function.identity()));
		// La consulta ya viene ordenada por nombre de producto; LinkedHashMap conserva ese orden.
		Map<Product, List<Series>> byProduct = series.findByProduct_ActiveTrueOrderByProduct_NameAscIdAsc().stream()
				.collect(Collectors.groupingBy(Series::getProduct, LinkedHashMap::new, Collectors.toList()));
		return byProduct.entrySet().stream()
				.map(entry -> new ProductSummary(entry.getKey().getId(), entry.getKey().getCode(),
						entry.getKey().getName(),
						entry.getValue().stream().map(s -> summary(s, stats.get(s.getId()))).toList()))
				.toList();
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('history:read')")
	public SeriesObservations observations(Long seriesId) {
		Series found = series.findWithProductById(seriesId).orElseThrow(() -> notFound(seriesId));
		Long dataVersion = observations.summarize(seriesId).map(SeriesStats::dataVersion).orElse(null);
		List<ObservationValue> values = observations.findBySeries_IdOrderByPeriodAsc(seriesId).stream()
				.map(o -> ObservationValue.of(o.getPeriod(), o.getValue()))
				.toList();
		return new SeriesObservations(found.getId(), found.getProduct().getCode(), found.getProduct().getName(),
				found.getMeasure(), found.getPeriodicity(), found.getUnit(), dataVersion, values);
	}

	/**
	 * Agrega el lote completo o nada (una transacción). La serie queda bloqueada mientras se valida, para
	 * que dos cargas simultáneas no dejen huecos ni repitan meses.
	 */
	@PreAuthorize("hasAuthority('history:write')")
	public LoadSummary load(Long seriesId, List<NewObservation> batch, Long actorId) {
		Series target = series.findForUpdateById(seriesId).orElseThrow(() -> notFound(seriesId));
		List<YearMonth> periods = batch.stream().map(NewObservation::period).toList();
		List<YearMonth> existing = observations.findPeriodsBySeriesId(seriesId).stream().map(YearMonth::from).toList();
		MonthlyLoadRules.check(existing, periods, YearMonth.now(COMPANY_ZONE));

		YearMonth first = periods.stream().min(Comparator.naturalOrder()).orElseThrow();
		YearMonth last = periods.stream().max(Comparator.naturalOrder()).orElseThrow();
		DataLoad load = loads.save(new DataLoad(target, actorId, batch.size(), first, last));
		observations.saveAll(batch.stream()
				.sorted(Comparator.comparing(NewObservation::period))
				.map(o -> new Observation(target, load, o.period(), o.value()))
				.toList());
		return new LoadSummary(load.getId(), seriesId, load.getCount(), first, last, load.getLoadedAt());
	}

	private static SeriesSummary summary(Series s, SeriesStats stats) {
		if (stats == null) {
			return new SeriesSummary(s.getId(), s.getMeasure(), s.getPeriodicity(), s.getUnit(), null, null, 0, null);
		}
		return new SeriesSummary(s.getId(), s.getMeasure(), s.getPeriodicity(), s.getUnit(),
				month(stats.firstPeriod()), month(stats.lastPeriod()), stats.count(), stats.dataVersion());
	}

	private static YearMonth month(LocalDate date) {
		return YearMonth.from(date);
	}

	private static ResourceNotFoundException notFound(Long seriesId) {
		return new ResourceNotFoundException("No existe la serie con id " + seriesId + ".");
	}
}
