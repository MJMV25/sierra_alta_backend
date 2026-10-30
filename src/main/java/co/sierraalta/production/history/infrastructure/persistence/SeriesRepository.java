package co.sierraalta.production.history.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import co.sierraalta.production.history.domain.Series;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeriesRepository extends JpaRepository<Series, Long> {

	@EntityGraph(attributePaths = "product")
	List<Series> findByProduct_ActiveTrueOrderByProduct_NameAscIdAsc();

	@EntityGraph(attributePaths = "product")
	Optional<Series> findWithProductById(Long id);

	/**
	 * Bloquea la serie durante la carga: dos cargas simultáneas se validan una después de la otra, así
	 * que no pueden dejar huecos entre ellas.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Series s where s.id = :id")
	Optional<Series> findForUpdateById(@Param("id") Long id);
}
