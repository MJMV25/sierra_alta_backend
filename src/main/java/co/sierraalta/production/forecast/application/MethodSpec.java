package co.sierraalta.production.forecast.application;

import java.math.BigDecimal;
import java.util.List;

import co.sierraalta.production.forecast.domain.ExponentialSmoothing;
import co.sierraalta.production.forecast.domain.ForecastMethod;
import co.sierraalta.production.forecast.domain.InvalidForecastParameterException;
import co.sierraalta.production.forecast.domain.LinearRegression;
import co.sierraalta.production.forecast.domain.MethodCode;
import co.sierraalta.production.forecast.domain.SeasonalMultiplicative;
import co.sierraalta.production.forecast.domain.SimpleMovingAverage;
import co.sierraalta.production.forecast.domain.TrendAdjustedExponentialSmoothing;
import co.sierraalta.production.forecast.domain.WeightedMovingAverage;

/**
 * Método pedido con sus parámetros. Solo se usan los que corresponden al método: n para el promedio simple,
 * pesos para el ponderado, α para el suavizamiento, α y β para Holt y L para el estacional; la regresión
 * lineal no tiene. Los demás se ignoran.
 */
public record MethodSpec(MethodCode method, Integer periods, List<BigDecimal> weights, BigDecimal alpha,
		BigDecimal beta, Integer seasonLength) {

	/** Construye el algoritmo; los parámetros faltantes o fuera de rango se informan con su campo. */
	ForecastMethod toAlgorithm() {
		return switch (method) {
			case SIMPLE_MOVING_AVERAGE -> new SimpleMovingAverage(required("periods", periods));
			case WEIGHTED_MOVING_AVERAGE -> new WeightedMovingAverage(
					required("weights", weights).stream().map(BigDecimal::doubleValue).toList());
			case EXPONENTIAL_SMOOTHING -> new ExponentialSmoothing(required("alpha", alpha).doubleValue());
			case TREND_ADJUSTED_EXPONENTIAL_SMOOTHING -> new TrendAdjustedExponentialSmoothing(
					required("alpha", alpha).doubleValue(), required("beta", beta).doubleValue());
			case LINEAR_REGRESSION -> new LinearRegression();
			case SEASONAL_MULTIPLICATIVE -> new SeasonalMultiplicative(required("seasonLength", seasonLength));
		};
	}

	Integer storedSeasonLength() {
		return method == MethodCode.SEASONAL_MULTIPLICATIVE ? seasonLength : null;
	}

	/** n guardado: el del promedio simple o la cantidad de pesos del ponderado. */
	Integer storedPeriods() {
		return switch (method) {
			case SIMPLE_MOVING_AVERAGE -> periods;
			case WEIGHTED_MOVING_AVERAGE -> weights.size();
			default -> null;
		};
	}

	List<BigDecimal> storedWeights() {
		return method == MethodCode.WEIGHTED_MOVING_AVERAGE ? weights : List.of();
	}

	BigDecimal storedAlpha() {
		return method == MethodCode.EXPONENTIAL_SMOOTHING || method == MethodCode.TREND_ADJUSTED_EXPONENTIAL_SMOOTHING
				? alpha : null;
	}

	BigDecimal storedBeta() {
		return method == MethodCode.TREND_ADJUSTED_EXPONENTIAL_SMOOTHING ? beta : null;
	}

	private static <T> T required(String field, T value) {
		if (value == null) {
			throw new InvalidForecastParameterException(field, "Este método requiere el parámetro.");
		}
		return value;
	}
}
