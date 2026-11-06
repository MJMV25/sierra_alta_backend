package co.sierraalta.production.history.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import co.sierraalta.production.history.domain.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObservationRepository extends JpaRepository<Observation, Long> {

	List<Observation> findBySeries_IdOrderByPeriodAsc(Long seriesId);

	@Query("select o.period from Observation o where o.series.id = :seriesId")
	List<LocalDate> findPeriodsBySeriesId(@Param("seriesId") Long seriesId);

	@Query("""
			select new co.sierraalta.production.history.infrastructure.persistence.SeriesStats(
				o.series.id, min(o.period), max(o.period), count(o), max(o.load.id))
			from Observation o group by o.series.id""")
	List<SeriesStats> summarizeAll();

	@Query("""
			select new co.sierraalta.production.history.infrastructure.persistence.SeriesStats(
				o.series.id, min(o.period), max(o.period), count(o), max(o.load.id))
			from Observation o where o.series.id = :seriesId group by o.series.id""")
	Optional<SeriesStats> summarize(@Param("seriesId") Long seriesId);
}
