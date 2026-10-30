/**
 * Módulo de pronósticos: coordina la ejecución y conserva entradas y resultados.
 *
 * <p>El servicio {@code EjecutarPronostico} actuará como Facade (ADR 04): consulta la serie mediante el
 * servicio público de {@code history}, ejecuta los métodos ({@code domain.ForecastMethod}, Strategy),
 * entrega los pares real-pronosticado a {@code indicators} y guarda ejecución, instantánea de entradas,
 * resultados y métricas en una sola transacción. {@code indicators} nunca invoca a este módulo, para
 * evitar ciclos.
 *
 * <p>Estado: los cuatro métodos del ANEXO A están implementados en {@code domain} y verificados contra el
 * Excel corregido (ADR 07). Pendientes: caso de uso, persistencia de parámetros y endpoints.
 */
package co.sierraalta.production.forecast;
