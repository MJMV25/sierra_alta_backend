/**
 * Módulo de pronósticos: coordina la ejecución y conserva entradas y resultados.
 *
 * <p>{@code application.ForecastService} es el Facade (ADR 04): consulta la serie mediante el servicio
 * público de {@code history}, ejecuta los métodos ({@code domain.ForecastMethod}, Strategy), entrega los
 * pares real-pronosticado a {@code indicators} y guarda ejecución, instantánea de entradas, resultados y
 * métricas en una sola transacción. {@code indicators} nunca invoca a este módulo, para evitar ciclos.
 *
 * <p>Estado: funcional (fase 2). Los cuatro métodos reproducen el Excel corregido también por la API
 * ({@code ForecastApiTest}). Las ejecuciones son inmutables ({@code @Immutable}).
 */
package co.sierraalta.production.forecast;
