package co.sierraalta.production.history.domain;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import co.sierraalta.production.shared.error.BusinessRuleException;
import co.sierraalta.production.shared.error.ConflictException;

/**
 * Reglas para agregar meses a una serie (ADR 07). Los métodos de pronóstico suponen meses consecutivos,
 * así que una serie nunca queda con huecos: la carga puede extenderla hacia adelante (el caso normal,
 * un mes nuevo) o hacia atrás, pero no dejar meses sin dato ni repetir uno ya cargado.
 */
public final class MonthlyLoadRules {

	/** Meses que se listan como máximo en un mensaje de error. */
	private static final int LISTED_PERIODS = 12;

	private MonthlyLoadRules() {
	}

	/**
	 * @param existing     meses ya cargados en la serie (consecutivos por esta misma regla)
	 * @param incoming     meses de la carga, en cualquier orden
	 * @param currentMonth mes en curso; no se aceptan meses posteriores
	 */
	public static void check(Collection<YearMonth> existing, List<YearMonth> incoming, YearMonth currentMonth) {
		Set<YearMonth> unique = new TreeSet<>(incoming);
		if (unique.size() < incoming.size()) {
			Set<YearMonth> seen = new TreeSet<>();
			throw new BusinessRuleException("La carga repite los periodos "
					+ list(incoming.stream().filter(period -> !seen.add(period)).distinct()) + ".");
		}
		List<YearMonth> future = unique.stream().filter(period -> period.isAfter(currentMonth)).toList();
		if (!future.isEmpty()) {
			throw new BusinessRuleException("No se pueden cargar meses posteriores a " + currentMonth + ": "
					+ list(future.stream()) + ".");
		}
		List<YearMonth> repeated = unique.stream().filter(existing::contains).toList();
		if (!repeated.isEmpty()) {
			throw new ConflictException("La serie ya tiene datos de " + list(repeated.stream()) + ".");
		}
		TreeSet<YearMonth> all = new TreeSet<>(existing);
		all.addAll(unique);
		List<YearMonth> missing = Stream.iterate(all.first(), period -> !period.isAfter(all.last()),
				period -> period.plusMonths(1)).filter(period -> !all.contains(period)).toList();
		if (!missing.isEmpty()) {
			throw new BusinessRuleException("La serie quedaría sin datos en " + list(missing.stream())
					+ ". Cargue los meses consecutivos; un mes sin producción se registra con 0.");
		}
	}

	private static String list(Stream<YearMonth> periods) {
		List<String> all = periods.map(YearMonth::toString).toList();
		String listed = all.stream().limit(LISTED_PERIODS).collect(Collectors.joining(", "));
		return all.size() > LISTED_PERIODS ? listed + " y " + (all.size() - LISTED_PERIODS) + " más" : listed;
	}
}
