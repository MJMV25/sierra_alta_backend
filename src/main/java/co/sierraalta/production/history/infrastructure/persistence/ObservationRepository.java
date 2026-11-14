package co.sierraalta.production.history.infrastructure.persistence;

import java.util.List;

import co.sierraalta.production.history.domain.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ObservationRepository extends JpaRepository<Observation, Long> {

	List<Observation> findBySeries_IdOrderByPeriodAsc(Long seriesId);

	@Query("""
			select new co.sierraalta.production.history.infrastructure.persistence.SeriesStats(
				o.series.id, min(o.period), max(o.period), count(o))
			from Observation o group by o.series.id""")
	List<SeriesStats> summarizeAll();
}
