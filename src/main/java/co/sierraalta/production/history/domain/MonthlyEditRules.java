package co.sierraalta.production.history.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import co.sierraalta.production.shared.error.BusinessRuleException;

/**
 * Reglas para editar la hoja de una serie mensual (ADR 07 y 13). Los métodos de pronóstico suponen meses
 * consecutivos, así que la serie nunca queda con huecos: se pueden corregir meses, agregar meses antes o
 * después de los que hay y borrar solo los últimos. Un mes sin producción se registra con 0.
 */
public final class MonthlyEditRules {

	/** Meses que se listan como máximo en un mensaje de error. */
	private static final int LISTED_PERIODS = 12;

	private MonthlyEditRules() {
	}

	/**
	 * @param existing     valores actuales de la serie por mes
	 * @param changes      lo pedido, en cualquier orden
	 * @param currentMonth mes en curso; no se aceptan meses posteriores
	 * @return los cambios efectivos ordenados por mes; un mes que queda con el mismo valor no aparece
	 */
	public static List<PlannedChange> plan(Map<YearMonth, BigDecimal> existing, List<ObservationChange> changes,
			YearMonth currentMonth) {
		Set<YearMonth> seen = new TreeSet<>();
		List<YearMonth> repeated = changes.stream().map(ObservationChange::period).filter(p -> !seen.add(p))
				.distinct().toList();
		if (!repeated.isEmpty()) {
			throw new BusinessRuleException("El cambio repite los meses " + list(repeated.stream()) + ".");
		}
		List<YearMonth> future = changes.stream().filter(c -> !c.removes() && c.period().isAfter(currentMonth))
				.map(ObservationChange::period).sorted().toList();
		if (!future.isEmpty()) {
			throw new BusinessRuleException("No se pueden registrar meses posteriores a " + currentMonth + ": "
					+ list(future.stream()) + ".");
		}
		List<YearMonth> missing = changes.stream().filter(c -> c.removes() && !existing.containsKey(c.period()))
				.map(ObservationChange::period).sorted().toList();
		if (!missing.isEmpty()) {
			throw new BusinessRuleException("No hay datos que borrar en " + list(missing.stream()) + ".");
		}

		List<PlannedChange> planned = new ArrayList<>();
		for (ObservationChange change : changes) {
			BigDecimal previous = existing.get(change.period());
			if (previous != null && change.value() != null && previous.compareTo(change.value()) == 0) {
				continue;
			}
			planned.add(new PlannedChange(change.period(), previous, change.value()));
		}
		planned.sort(Comparator.comparing(PlannedChange::period));
		checkResultingSeries(existing.keySet(), planned);
		return planned;
	}

	private static void checkResultingSeries(Set<YearMonth> existing, List<PlannedChange> planned) {
		TreeSet<YearMonth> result = new TreeSet<>(existing);
		planned.stream().filter(PlannedChange::removes).map(PlannedChange::period).forEach(result::remove);
		planned.stream().filter(PlannedChange::adds).map(PlannedChange::period).forEach(result::add);
		if (result.isEmpty()) {
			return;
		}
		List<YearMonth> removedInside = planned.stream().filter(PlannedChange::removes).map(PlannedChange::period)
				.filter(period -> period.isBefore(result.last())).toList();
		if (!removedInside.isEmpty()) {
			throw new BusinessRuleException("Solo se pueden borrar los últimos meses de la serie; "
					+ list(removedInside.stream()) + " tiene meses posteriores. Si no hubo producción, escriba 0.");
		}
		List<YearMonth> gaps = Stream.iterate(result.first(), period -> !period.isAfter(result.last()),
				period -> period.plusMonths(1)).filter(period -> !result.contains(period)).toList();
		if (!gaps.isEmpty()) {
			throw new BusinessRuleException("La serie quedaría sin datos en " + list(gaps.stream())
					+ ". Registre los meses seguidos; un mes sin producción se registra con 0.");
		}
	}

	private static String list(Stream<YearMonth> periods) {
		List<String> all = periods.map(YearMonth::toString).toList();
		String listed = all.stream().limit(LISTED_PERIODS).collect(Collectors.joining(", "));
		return all.size() > LISTED_PERIODS ? listed + " y " + (all.size() - LISTED_PERIODS) + " más" : listed;
	}
}
