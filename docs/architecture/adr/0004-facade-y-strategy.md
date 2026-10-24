# ADR 04 — Coordinar el pronóstico con Facade y aplazar Strategy

**Estado:** propuesto

**Contexto.** El recorrido reúne históricos, cálculo, indicadores y guardado, pero solo hay un método
inicial definido (promedio móvil simple de dos periodos).

**Alternativas.** Coordinación duplicada en controladores · un servicio de aplicación · jerarquía
anticipada de algoritmos.

**Decisión.** El servicio `EjecutarPronostico` (módulo `forecast`) actuará como Facade; el promedio
móvil será un componente de dominio aislado. Strategy se evaluará al aprobarse un segundo método.

**Consecuencias.**
- (+) Un punto de coordinación y fórmulas comprobables con menos abstracciones iniciales.
- (−) Un nuevo método exigirá extraer el contrato común; se acepta para no diseñar sobre requisitos no validados.
- Referencia de verificación: serie 100, 120, 110, 130 → pronósticos 110 y 115, estimación 120,
  MAD 7,50 y MAPE ≈ 5,769231 %.
