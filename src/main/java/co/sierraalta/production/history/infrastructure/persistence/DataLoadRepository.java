package co.sierraalta.production.history.infrastructure.persistence;

import java.util.List;

import co.sierraalta.production.history.domain.DataLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * La versión de datos de una serie es su última carga, no la carga de sus observaciones: borrar el último mes
 * también crea una carga y la versión siempre avanza.
 */
public interface DataLoadRepository extends JpaRepository<DataLoad, Long> {

	@Query("""
			select new co.sierraalta.production.history.infrastructure.persistence.LatestLoad(c.series.id, max(c.id))
			from DataLoad c group by c.series.id""")
	List<LatestLoad> findLatestOfEachSeries();

	/** Nulo si la serie nunca ha tenido datos. */
	@Query("select max(c.id) from DataLoad c where c.series.id = :seriesId")
	Long findLatestId(@Param("seriesId") Long seriesId);
}
