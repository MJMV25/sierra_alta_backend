/**
 * Módulo de pronósticos: coordina la ejecución y conserva entradas y resultados.
 *
 * <p>El servicio {@code EjecutarPronostico} actuará como Facade (ADR 04): consulta la serie mediante el
 * servicio público de {@code history}, invoca el cálculo de promedio móvil simple (componente de dominio
 * aislado, sin dependencias web ni SQL), entrega los pares real-pronosticado a {@code indicators} y guarda
 * ejecución, instantánea de entradas, resultados y métricas en una sola transacción.
 *
 * <p>Strategy se aplaza hasta que exista un segundo método validado. {@code indicators} nunca invoca a
 * este módulo, para evitar ciclos.
 *
 * <p>Estado: esquema creado (migración V2) y endpoints protegidos por rol; pendiente de implementación.
 */
package co.sierraalta.production.forecast;
