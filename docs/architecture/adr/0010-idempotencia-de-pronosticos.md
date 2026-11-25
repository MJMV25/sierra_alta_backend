# ADR 10 — Ejecución de pronósticos idempotente con `Idempotency-Key`

**Estado:** aceptado

**Contexto.** Cada ejecución queda guardada con su instantánea ([ADR 03](0003-postgresql-y-trazabilidad.md)).
Un doble clic, un reintento del navegador o una red lenta pueden repetir `POST /api/v1/forecasts` y crear
ejecuciones duplicadas que confunden el historial. Deshabilitar el botón ayuda, pero no protege el servidor
(regla R28 del proyecto).

**Alternativas.** Solo deshabilitar el botón en React · rechazar ejecuciones iguales en un intervalo ·
clave de idempotencia enviada por el cliente.

**Decisión.**
1. `POST /api/v1/forecasts` acepta la cabecera opcional `Idempotency-Key` (1 a 64 caracteres
   `A-Z a-z 0-9 _ -`). Un formato inválido responde 400 con error en ese campo.
2. `ForecastService` busca una ejecución del mismo usuario con esa clave: si existe, la devuelve con 200
   sin recalcular; si no, calcula, guarda la clave en `ejecucion.clave_idempotencia` y responde 201.
3. El frontend genera una clave nueva (UUID) por cada clic en "Ejecutar"; si la red reintenta esa
   solicitud, el backend devuelve la ejecución original.

**Consecuencias.**
- (+) Escenario QT3: un reenvío no crea ejecuciones duplicadas.
- (+) La clave es por usuario: dos usuarios no chocan aunque generen la misma.
- (−) Si un cliente reutiliza una clave con otra solicitud, recibe la ejecución anterior; el frontend
  debe generar siempre una clave nueva por solicitud distinta.
- (−) Las claves no caducan; se revisará si el historial crece lo suficiente para necesitarlo.
