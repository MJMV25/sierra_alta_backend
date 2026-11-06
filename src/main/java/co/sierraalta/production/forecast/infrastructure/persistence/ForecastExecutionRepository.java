package co.sierraalta.production.forecast.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import co.sierraalta.production.forecast.domain.ForecastExecution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastExecutionRepository extends JpaRepository<ForecastExecution, Long> {

	Optional<ForecastExecution> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

	@EntityGraph(attributePaths = "methods")
	List<ForecastExecution> findTop50ByOrderByExecutedAtDescIdDesc();

	@EntityGraph(attributePaths = "methods")
	List<ForecastExecution> findTop50BySeriesIdOrderByExecutedAtDescIdDesc(Long seriesId);
}
