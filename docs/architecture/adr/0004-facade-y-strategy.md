# ADR 04 — Coordinar el pronóstico con Facade y Strategy

**Estado:** aceptado (Strategy aprobado el 29-10-2026)

**Contexto.** El recorrido reúne históricos, cálculo, indicadores y guardado. La primera versión de
este ADR aplazó Strategy porque solo había un método definido. El ANEXO A corregido y las respuestas de
Industrial (29-10-2026) fijan cuatro métodos: promedio móvil simple, promedio móvil ponderado,
suavizamiento exponencial y suavizamiento exponencial con ajuste a la tendencia (Holt).

**Alternativas.** Coordinación duplicada en controladores · un servicio de aplicación con un `switch`
por método · un contrato común con una implementación por método.

**Decisión.**
1. `ForecastService` (módulo `forecast`, caso de uso "ejecutar pronóstico") actúa como Facade: consulta
   la serie con `HistoryService`, ejecuta los métodos pedidos, calcula MAD y MAPE con `indicators` en la
   ventana común y guarda ejecución, entradas, resultados y métricas en una transacción (migración V4).
2. Strategy: `ForecastMethod` (paquete `forecast.domain`) con `SimpleMovingAverage`,
   `WeightedMovingAverage`, `ExponentialSmoothing` y `TrendAdjustedExponentialSmoothing`. Cada uno valida
   sus parámetros al construirse y no depende de Spring ni de SQL.
3. MAD y MAPE se calculan solo en `indicators.domain.ErrorIndicators`. Las reglas de cálculo están en
   el [ADR 07](0007-reglas-de-calculo.md).

**Consecuencias.**
- (+) Un método nuevo (regresión lineal o estacional multiplicativo, previstos en el documento) se
  agrega con una implementación más, sin tocar los demás.
- (+) Fórmulas comprobables sin base de datos: `ExcelReferenceTest` compara cada periodo con el Excel.
- (−) Una interfaz más que mantener; se justifica porque ya hay cuatro implementaciones.
- Referencias de verificación: hojas CASO 1 SPOKANE y CASO 2 ACME del Excel corregido
  (`src/test/resources/forecast/golden`) y la serie 100, 120, 110, 130 → pronósticos 110 y 115,
  estimación 120, MAD 7,50 y MAPE ≈ 5,769231 %.
