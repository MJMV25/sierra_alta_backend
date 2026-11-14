package co.sierraalta.production.history.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import co.sierraalta.production.auth.application.UserDirectory;
import co.sierraalta.production.history.domain.DataLoad;
import co.sierraalta.production.history.domain.MonthlyEditRules;
import co.sierraalta.production.history.domain.Observation;
import co.sierraalta.production.history.domain.ObservationChangeLog;
import co.sierraalta.production.history.domain.PlannedChange;
import co.sierraalta.production.history.domain.Product;
import co.sierraalta.production.history.domain.Series;
import co.sierraalta.production.history.infrastructure.persistence.DataLoadRepository;
import co.sierraalta.production.history.infrastructure.persistence.LatestLoad;
import co.sierraalta.production.history.infrastructure.persistence.ObservationChangeLogRepository;
import co.sierraalta.production.history.infrastructure.persistence.ObservationRepository;
import co.sierraalta.production.history.infrastructure.persistence.SeriesRepository;
import co.sierraalta.production.history.infrastructure.persistence.SeriesStats;
import co.sierraalta.production.shared.Decimals;
import co.sierraalta.production.shared.error.BusinessRuleException;
import co.sierraalta.production.shared.error.ConflictException;
import co.sierraalta.production.shared.error.InvalidFieldException;
import co.sierraalta.production.shared.error.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de históricos: consultar referencias y series, editar la hoja de datos (agregar, corregir y
 * borrar meses) y consultar el historial de cambios.
 *
 * <p>Es el servicio público del módulo: {@code forecast} lee las series por aquí, nunca por las tablas.
 * Cada operación exige su permiso aunque la ruta ya lo verifique en {@code SecurityConfig}.
 */
@Service
@Transactional
public class HistoryService {

	/** Zona de la empresa: define cuál es el mes en curso. */
	static final ZoneId COMPANY_ZONE = ZoneId.of("America/Bogota");

	/** Líneas del historial de cambios que se devuelven como máximo. */
	static final int CHANGE_HISTORY_LIMIT = 500;

	private final SeriesRepository series;
	private final ObservationRepository observations;
	private final DataLoadRepository loads;
	private final ObservationChangeLogRepository changes;
	private final UserDirectory users;

	public HistoryService(SeriesRepository series, ObservationRepository observations, DataLoadRepository loads,
			ObservationChangeLogRepository changes, UserDirectory users) {
		this.series = series;
		this.observations = observations;
		this.loads = loads;
		this.changes = changes;
		this.users = users;
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('history:read')")
	public List<ProductSummary> products() {
		Map<Long, SeriesStats> stats = observations.summarizeAll().stream()
				.collect(Collectors.toMap(SeriesStats::seriesId, Function.identity()));
		Map<Long, Long> versions = loads.findLatestOfEachSeries().stream()
				.collect(Collectors.toMap(LatestLoad::seriesId, LatestLoad::loadId));
		// La consulta ya viene ordenada por nombre de producto; LinkedHashMap conserva ese orden.
		Map<Product, List<Series>> byProduct = series.findByProduct_ActiveTrueOrderByProduct_NameAscIdAsc().stream()
				.collect(Collectors.groupingBy(Series::getProduct, LinkedHashMap::new, Collectors.toList()));
		return byProduct.entrySet().stream()
				.map(entry -> new ProductSummary(entry.getKey().getId(), entry.getKey().getCode(),
						entry.getKey().getName(), entry.getKey().isSample(),
						entry.getValue().stream()
								.map(s -> summary(s, stats.get(s.getId()), versions.get(s.getId())))
								.toList()))
				.toList();
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('history:read')")
	public SeriesObservations observations(Long seriesId) {
		Series found = series.findWithProductById(seriesId).orElseThrow(() -> notFound(seriesId));
		List<ObservationValue> values = observations.findBySeries_IdOrderByPeriodAsc(seriesId).stream()
				.map(o -> ObservationValue.of(o.getPeriod(), o.getValue()))
				.toList();
		return new SeriesObservations(found.getId(), found.getProduct().getCode(), found.getProduct().getName(),
				found.getMeasure(), found.getPeriodicity(), found.getUnit(), loads.findLatestId(seriesId), values);
	}

	/** Identificación de las series pedidas, por id; las que no existen no aparecen en el mapa. */
	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('history:read')")
	public Map<Long, SeriesReference> seriesReferences(Collection<Long> seriesIds) {
		return series.findWithProductByIdIn(seriesIds).stream()
				.collect(Collectors.toMap(Series::getId, s -> new SeriesReference(s.getId(),
						s.getProduct().getCode(), s.getProduct().getName(), s.getMeasure(), s.getPeriodicity(),
						s.getUnit())));
	}

	/**
	 * Guarda los cambios de la hoja: todas las series o ninguna (una transacción). Cada serie queda bloqueada
	 * mientras se valida, en orden de id para que dos guardados simultáneos no se bloqueen entre sí. Una
	 * serie cuyos cambios no cambian nada no genera carga ni nueva versión.
	 */
	@PreAuthorize("hasAuthority('history:write')")
	public List<EditSummary> edit(List<SeriesEdit> edits, Long actorId) {
		Set<Long> seen = new HashSet<>();
		for (int i = 0; i < edits.size(); i++) {
			if (!seen.add(edits.get(i).seriesId())) {
				throw new InvalidFieldException("series[" + i + "].seriesId", "Esta serie ya está en la lista.");
			}
		}
		YearMonth currentMonth = YearMonth.now(COMPANY_ZONE);
		return edits.stream()
				.sorted(Comparator.comparing(SeriesEdit::seriesId))
				.map(edit -> apply(edit, actorId, currentMonth))
				.toList();
	}

	/** Últimos cambios de la serie, del más reciente al más antiguo. */
	@Transactional(readOnly = true)
	@PreAuthorize("hasAuthority('history:read')")
	public List<ChangeEntry> changes(Long seriesId) {
		if (!series.existsById(seriesId)) {
			throw notFound(seriesId);
		}
		List<ObservationChangeLog> found = changes.findLatestBySeries(seriesId,
				PageRequest.of(0, CHANGE_HISTORY_LIMIT));
		Map<Long, String> usernames = users.usernames(
				found.stream().map(change -> change.getLoad().getUserId()).distinct().toList());
		return found.stream()
				.map(change -> new ChangeEntry(change.getLoad().getId(), change.getPeriod(),
						Decimals.plain(change.getPrevious()), Decimals.plain(change.getNext()), change.getLoad().getLoadedAt(),
						usernames.get(change.getLoad().getUserId())))
				.toList();
	}

	private EditSummary apply(SeriesEdit edit, Long actorId, YearMonth currentMonth) {
		Series target = series.findForUpdateById(edit.seriesId()).orElseThrow(() -> notFound(edit.seriesId()));
		if (target.getProduct().isSample()) {
			throw new BusinessRuleException("La serie " + target.getProduct().getName()
					+ " es un dato de ejemplo del Excel y no se puede modificar.");
		}
		Long currentVersion = loads.findLatestId(target.getId());
		if (edit.expectedDataVersion() != null && !edit.expectedDataVersion().equals(currentVersion)) {
			throw new ConflictException("Los datos de " + target.getProduct().getName()
					+ " cambiaron desde que abrió la hoja. Recargue la página para ver la versión actual.");
		}
		Map<YearMonth, Observation> existing = observations.findBySeries_IdOrderByPeriodAsc(target.getId()).stream()
				.collect(Collectors.toMap(Observation::getPeriod, Function.identity()));
		Map<YearMonth, BigDecimal> values = existing.values().stream()
				.collect(Collectors.toMap(Observation::getPeriod, Observation::getValue));
		List<PlannedChange> planned = MonthlyEditRules.plan(values, edit.changes(), currentMonth);
		if (planned.isEmpty()) {
			return new EditSummary(target.getId(), currentVersion, 0, 0, 0);
		}

		DataLoad load = loads.save(new DataLoad(target, actorId, planned.size(), planned.getFirst().period(),
				planned.getLast().period()));
		for (PlannedChange change : planned) {
			if (change.adds()) {
				observations.save(new Observation(target, load, change.period(), change.next()));
			}
			else if (change.removes()) {
				observations.delete(existing.get(change.period()));
			}
			else {
				existing.get(change.period()).correct(change.next(), load);
			}
		}
		changes.saveAll(planned.stream().map(change -> new ObservationChangeLog(load, change)).toList());
		return new EditSummary(target.getId(), load.getId(), count(planned, PlannedChange::adds),
				count(planned, change -> !change.adds() && !change.removes()), count(planned, PlannedChange::removes));
	}

	private static int count(List<PlannedChange> planned, Predicate<PlannedChange> filter) {
		return (int) planned.stream().filter(filter).count();
	}

	private static SeriesSummary summary(Series s, SeriesStats stats, Long dataVersion) {
		if (stats == null) {
			return new SeriesSummary(s.getId(), s.getMeasure(), s.getPeriodicity(), s.getUnit(), null, null, 0,
					dataVersion);
		}
		return new SeriesSummary(s.getId(), s.getMeasure(), s.getPeriodicity(), s.getUnit(),
				month(stats.firstPeriod()), month(stats.lastPeriod()), stats.count(), dataVersion);
	}

	private static YearMonth month(LocalDate date) {
		return YearMonth.from(date);
	}

	private static ResourceNotFoundException notFound(Long seriesId) {
		return new ResourceNotFoundException("No existe la serie con id " + seriesId + ".");
	}
}
