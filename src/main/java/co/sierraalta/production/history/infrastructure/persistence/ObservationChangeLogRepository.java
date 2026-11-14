package co.sierraalta.production.history.infrastructure.persistence;

import java.util.List;

import co.sierraalta.production.history.domain.ObservationChangeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObservationChangeLogRepository extends JpaRepository<ObservationChangeLog, Long> {

	/** Cambios de una serie, del guardado más reciente al más antiguo y por mes dentro de cada guardado. */
	@EntityGraph(attributePaths = "load")
	@Query("""
			select ch from ObservationChangeLog ch
			where ch.load.series.id = :seriesId
			order by ch.load.id desc, ch.period asc""")
	List<ObservationChangeLog> findLatestBySeries(@Param("seriesId") Long seriesId, Pageable page);
}
