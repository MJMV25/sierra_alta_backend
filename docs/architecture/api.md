# API REST y alcance por rol

Versión vigente: **`/api/v1`** (constante `ApiPaths.V1`). Un cambio incompatible del contrato se
publica como `/api/v2` y convive con la anterior mientras el frontend migra; los cambios compatibles
(campos nuevos opcionales, endpoints nuevos) se agregan a la versión vigente. `/actuator/health` no
se versiona.

Formato: JSON. Errores: `application/problem+json` (RFC 9457) con un campo adicional `code`:

```json
{ "type": "urn:sierra-alta:problem:access-denied", "title": "Forbidden", "status": 403,
  "detail": "Su rol no tiene permiso para realizar esta operación.", "code": "access-denied",
  "instance": "/api/v1/forecasts" }
```

Los errores de validación (`code: "validation"`, 400) incluyen `errors: [{ "field", "message" }]`
para mostrarlos junto a cada campo del formulario.

| HTTP | `code`                    | Significado                                           |
|------|---------------------------|-------------------------------------------------------|
| 400  | `validation`              | Datos inválidos (ver `errors`)                        |
| 400  | `malformed-request`       | El cuerpo no es JSON válido o no tiene el formato esperado |
| 400  | `invalid-parameter`       | Parámetro de ruta o de consulta ausente o con tipo incorrecto |
| 401  | `invalid-credentials`     | Usuario/contraseña incorrectos o cuenta inactiva (mismo mensaje) |
| 401  | `authentication-required` | No hay sesión válida                                  |
| 403  | `access-denied`           | El rol no concede la operación (se audita como `ACCESS_DENIED`) |
| 403  | `csrf-invalid`            | Falta, venció o no coincide el token CSRF (se audita como `CSRF_REJECTED`); el frontend debe pedir un token nuevo y reintentar |
| 404  | `not-found`               | Recurso inexistente                                   |
| 405  | `method-not-allowed`      | Método HTTP no disponible para la ruta               |
| 409  | `conflict`                | Nombre único repetido o violación de integridad       |
| 415  | `unsupported-media-type`  | El cuerpo no se envió como `application/json`         |
| 422  | `business-rule`           | Regla de negocio (p. ej. desactivarse a sí mismo)     |
| 500  | `internal-error`          | Error inesperado (el detalle solo queda en el log)    |
| 501  | `not-implemented`         | Endpoint con permisos definidos, lógica pendiente     |

## Sesión y CSRF (guía para el frontend React)

1. `GET /api/v1/auth/csrf` al cargar la app: deja la cookie legible `XSRF-TOKEN`.
2. En **toda** solicitud `POST/PUT/PATCH/DELETE` enviar la cabecera `X-XSRF-TOKEN` con el valor de esa
   cookie, y usar `credentials: 'include'` (Fetch) o `withCredentials: true` (Axios).
   Axios lo hace solo con `xsrfCookieName: 'XSRF-TOKEN'` y `xsrfHeaderName: 'X-XSRF-TOKEN'`.
3. `POST /api/v1/auth/login` crea la sesión (cookie `JSESSIONID`, `HttpOnly`) y **renueva** `XSRF-TOKEN`:
   relea la cookie después del login.
4. `GET /api/v1/auth/me` al recargar la página para recuperar la sesión (401 si no hay).
5. `POST /api/v1/auth/logout` → 204.

La sesión expira tras 30 minutos de inactividad. Si un administrador desactiva una cuenta, su sesión
abierta termina en la siguiente solicitud; si le cambia el rol, el nuevo alcance aplica de inmediato.

## Endpoints

### auth (funcional)

| Método | Ruta                               | Acceso          | Cuerpo / respuesta |
|--------|------------------------------------|-----------------|--------------------|
| GET    | `/api/v1/auth/csrf`                   | Público         | → `{headerName, token}` |
| POST   | `/api/v1/auth/login`                  | Público + CSRF  | `{username, password}` → `SessionResponse` |
| GET    | `/api/v1/auth/me`                     | Sesión          | → `SessionResponse` |
| POST   | `/api/v1/auth/logout`                 | CSRF            | → 204 |
| GET    | `/api/v1/admin/roles`                 | `users:manage`  | → `[{code, name, permissions[]}]` |
| GET    | `/api/v1/admin/users`                 | `users:manage`  | → `[UserSummary]` |
| POST   | `/api/v1/admin/users`                 | `users:manage`  | `{username, password, role}` → 201 `UserSummary` |
| PATCH  | `/api/v1/admin/users/{id}/role`       | `users:manage`  | `{role}` → `UserSummary` |
| PATCH  | `/api/v1/admin/users/{id}/status`     | `users:manage`  | `{active}` → `UserSummary` |
| GET    | `/api/v1/admin/security-events?limit=`| `users:manage`  | → eventos recientes (máx. 500) |
| GET    | `/actuator/health`                 | Público         | → `{status}` |

`SessionResponse`: `{ userId, username, role, roleName, permissions: ["forecast:read", ...] }`.
`UserSummary`: `{ id, username, role, roleName, active, createdAt }`.

Reglas de administración: no hay autorregistro; los nombres de usuario se guardan en minúsculas
(3–50 caracteres `a-z 0-9 . _ -`); contraseña de 10 a 72 caracteres; un administrador no puede
desactivarse ni cambiarse el rol a sí mismo; siempre queda al menos un administrador activo; las
cuentas no se eliminan, se desactivan.

### history (funcional)

| Método | Ruta                                          | Permiso         | Cuerpo / respuesta |
|--------|-----------------------------------------------|-----------------|--------------------|
| GET    | `/api/v1/history/products`                    | `history:read`  | → `[ProductSummary]` |
| GET    | `/api/v1/history/series/{id}/observations`    | `history:read`  | → `SeriesObservations` (404 si no existe) |
| GET    | `/api/v1/history/series/{id}/changes`         | `history:read`  | → últimos 500 `[ChangeEntry]` (404 si no existe) |
| PATCH  | `/api/v1/history/observations`                | `history:write` | `{series: [{seriesId, dataVersion?, changes: [{period, value}]}]}` → `[EditSummary]` |

`ProductSummary`: `{ id, code, name, sample, series: [{ id, measure, periodicity, unit, firstPeriod,
lastPeriod, observations, dataVersion }] }`; solo referencias activas, ordenadas por nombre. `sample` es
verdadero en los datos de ejemplo del Excel (Spokane y ACME), que no se pueden modificar. Una serie sin datos
trae `observations: 0` y periodos nulos.
`SeriesObservations`: `{ seriesId, productCode, productName, measure, periodicity, unit, dataVersion,
observations: [{ period: "2026-09", value: 155 }] }`, del mes más antiguo al más reciente.

Los periodos usan el formato `AAAA-MM`. `dataVersion` es el id de la última carga (guardado de la hoja)
de la serie: la "versión de datos" que acompaña a pronósticos y exportaciones. `measure` es `PRODUCCION`
en las 4 referencias de la empresa: la interfaz la rotula como producción (ADR 07).

**Guardar la hoja** ([ADR 13](adr/0013-hoja-de-datos-en-vivo.md)): cada cambio pone el valor de un mes
(lo agrega o lo corrige) o, con `value: null`, lo borra. Se guardan todas las series o ninguna; un mes
que queda con el mismo valor no cuenta como cambio. `EditSummary`: `{ seriesId, dataVersion, added,
corrected, removed }`. `ChangeEntry`: `{ loadId, period, previousValue, newValue, changedAt, changedBy }`
(`previousValue` nulo: mes agregado; `newValue` nulo: mes borrado).

| Caso | HTTP | `code` |
|------|------|--------|
| Sin series o sin cambios; más de 50 series o 600 meses por serie; serie repetida; periodo que no es `AAAA-MM`; cantidad negativa o no entera | 400 | `validation` (`errors[].field` como `series[0].changes[2].value`) |
| `dataVersion` distinta de la actual (otro guardó antes) | 409 | `conflict` |
| Datos de ejemplo; mes repetido; mes posterior al mes en curso (hora de Colombia); borrar un mes sin dato o que no es de los últimos; dejar meses sin dato | 422 | `business-rule` (el detalle nombra los meses) |

### forecast (funcional)

| Método | Ruta                          | Permiso            | Cuerpo / respuesta |
|--------|-------------------------------|--------------------|--------------------|
| POST   | `/api/v1/forecasts/preview`   | `forecast:read`    | `{seriesId, from?, to?, methods: [...], actuals?: [{period, value}]}` → `ForecastPreview` (no guarda nada) |
| POST   | `/api/v1/forecasts`           | `forecast:execute` | `{seriesId, from?, to?, methods: [...]}` → 201 `ForecastDetail` (200 si se reenvía la misma `Idempotency-Key`) |
| GET    | `/api/v1/forecasts?seriesId=` | `forecast:read`    | → últimas 50 `[ForecastSummary]`, de la más reciente a la más antigua; `seriesId` opcional |
| GET    | `/api/v1/forecasts/{id}`      | `forecast:read`    | → `ForecastDetail` (404 si no existe) |

Una ejecución corre de 1 a 6 métodos **distintos** sobre la misma serie y los evalúa en la misma ventana
(desde el primer periodo en que todos tienen pronóstico, como el Excel). Cada método lleva solo sus
parámetros; los demás se ignoran:

```json
{ "seriesId": 1, "methods": [
    { "method": "SIMPLE_MOVING_AVERAGE", "periods": 5 },
    { "method": "WEIGHTED_MOVING_AVERAGE", "weights": [0.3, 0.2, 0.2, 0.2, 0.1] },
    { "method": "EXPONENTIAL_SMOOTHING", "alpha": 0.05 },
    { "method": "TREND_ADJUSTED_EXPONENTIAL_SMOOTHING", "alpha": 0.0025, "beta": 0.99 } ] }
```

| Parámetro | Regla ([ADR 07](adr/0007-reglas-de-calculo.md)) |
|-----------|------------------------------------------------|
| `periods` (promedio simple) | Entero ≥ 1 y ≤ meses cargados. Con n igual al total solo hay pronóstico siguiente y los indicadores son nulos |
| `weights` (ponderado) | Cada uno en (0, 1], suma 1, hasta 10 decimales. `weights[0]` (K0) es el peso del mes **más reciente** |
| `alpha`, `beta` | Estrictamente entre 0 y 1, hasta 10 decimales |

`from` y `to` (opcionales, `AAAA-MM`) eligen el periodo histórico (requisito funcional 3 de la Entrega 1); sin
ellos se usa toda la serie. El pronóstico es del mes siguiente a `to`, aunque ese mes ya tenga dato cargado.

Errores: `from` o `to` fuera de los meses cargados o `to` anterior a `from` → 400 con `errors[].field`
`from`/`to`; parámetro faltante o fuera de rango, método repetido o `Idempotency-Key` con formato inválido →
400 `validation` con `errors[].field` como `methods[2].alpha`; serie sin datos → 422; serie inexistente → 404.

`ForecastDetail`: `{ id, seriesId, productCode, productName, measure, unit, dataVersion, executedAt,
executedBy, executedByUsername, periodFrom, periodTo, calculationVersion, evaluationStart, actuals: [{period, value}], methods: [MethodResult] }`.
`MethodResult`: `{ method, periods, weights, alpha, beta, seasonLength, forecasts: [{period, forecast, actual,
error, absolutePercentError}], nextForecast: {period, forecast, actual: null}, mad, mapePercent, pairsUsed,
mapeExcluded }`. `error` = real − pronóstico y `absolutePercentError` = |error| / real × 100 (nulo si el real
es 0), como las columnas |Et| y |Et|/Dt del Excel.

- `forecasts` trae todos los pronósticos que tienen real, también los anteriores a `evaluationStart`
  (el suavizamiento pronostica desde el mes 2); MAD y MAPE solo usan los meses desde `evaluationStart`.
- `mad` está en unidades; `mapePercent` en porcentaje. Ambos son nulos si no son calculables.
  `mapeExcluded` cuenta los meses con real 0, que no entran al MAPE.
- `dataVersion` es la carga de históricos usada y `actuals` la instantánea de reales: la ejecución no
  cambia aunque después se agreguen meses. `periodFrom` y `periodTo` son el primer y el último mes de
  `actuals`. `executedBy` es el id del usuario y `executedByUsername` su nombre (servicio público
  `auth.application.UserDirectory`; nulo si la cuenta ya no existe).
- `Idempotency-Key` (opcional, 1–64 caracteres `A-Z a-z 0-9 _ -`): el frontend envía una clave nueva por
  cada clic en "Ejecutar"; un reenvío con la misma clave devuelve la ejecución original sin recalcular.

`ForecastSummary`: `{ id, seriesId, productName, executedAt, executedBy, dataVersion, evaluationStart,
methods: ["SIMPLE_MOVING_AVERAGE", ...] }`.

**Vista previa** ([ADR 13](adr/0013-hoja-de-datos-en-vivo.md)): lo que la hoja muestra en vivo al cambiar un
parámetro o un dato. Mismas reglas y errores que guardar; además, `actuals` (opcional, hasta 1200) reemplaza
los datos guardados por los de la hoja sin guardar, y deben ser meses seguidos y sin repetir (si no, 400 con
`errors[].field` `actuals`). `ForecastPreview`: `{ seriesId, productCode, productName, measure, periodicity,
unit, dataVersion, draft, periodFrom, periodTo, evaluationStart, actuals, methods: [MethodResult],
comparison: ExecutionComparison }`; `dataVersion` es nulo y `draft` verdadero cuando se usaron `actuals`.

### indicators (funcional)

| Método | Ruta                                   | Permiso           | Respuesta |
|--------|----------------------------------------|-------------------|-----------|
| GET    | `/api/v1/indicators/executions/{id}`   | `indicators:read` | → `ExecutionComparison` (404 si la ejecución no existe) |

Tabla comparativa de los métodos de una ejecución guardada ([ADR 07](adr/0007-reglas-de-calculo.md) y
[ADR 13](adr/0013-hoja-de-datos-en-vivo.md)); la vista previa trae la misma estructura con `executionId` nulo:

```json
{ "executionId": 7, "seriesId": 1, "evaluationFrom": "2020-06", "evaluationTo": "2022-06",
  "bestByMad": ["TREND_ADJUSTED_EXPONENTIAL_SMOOTHING"], "bestByMape": ["TREND_ADJUSTED_EXPONENTIAL_SMOOTHING"],
  "methods": [ { "method": "SIMPLE_MOVING_AVERAGE", "mad": 20.584, "mapePercent": 7.5031047806,
                 "pairsUsed": 25, "mapeExcluded": 0, "comparable": true,
                 "bestByMad": false, "bestByMape": false }, ... ] }
```

- `evaluationFrom`–`evaluationTo`: meses evaluados; nulos si ningún mes tuvo pronóstico de todos los
  métodos. Se muestran siempre junto a los indicadores, porque la ventana común depende de los métodos
  ejecutados juntos.
- `bestByMad` y `bestByMape` señalan el menor de cada uno por separado (pueden ser métodos distintos).
  Si varios coinciden a 6 decimales, todos quedan marcados. Un indicador nulo nunca es el mejor.
- `comparable` es falso en el estacional multiplicativo: aparece en la tabla pero no compite por el mejor,
  porque se ajusta dentro de la muestra (ADR 12).
- Sin estado de aceptación ni umbral (se quitaron en el ADR 13).

Cualquier ruta no declarada en `SecurityConfig` se deniega (403 con sesión, 401 sin ella).

## Matriz de alcance por rol

Definida en `RoleCode` y verificada por `RoleScopeTest`. Parte de la tabla 2 del documento de arquitectura,
ajustada el 2026-11-05 ([ADR 05](adr/0005-alcance-de-roles.md): sin rol Supervisor de línea) y el 2026-11-14
([ADR 13](adr/0013-hoja-de-datos-en-vivo.md): solo el administrador edita la hoja y el analista guarda
pronósticos).

| Permiso            | ADMIN | PLANNER | ANALYST |
|--------------------|:-----:|:-------:|:-------:|
| `users:manage`     |  ✔    |         |         |
| `history:read`     |  ✔    |   ✔     |   ✔     |
| `history:write`    |  ✔    |         |         |
| `forecast:execute` |  ✔    |         |   ✔     |
| `forecast:read`    |  ✔    |   ✔     |   ✔     |
| `indicators:read`  |  ✔    |   ✔     |   ✔     |
| `forecast:export`  |  ✔    |   ✔     |         |

- **Administrador:** todos los permisos; es el único que edita la hoja de datos y administra cuentas.
- **Planeador:** consulta la hoja, recalcula en vivo, consulta pronósticos e indicadores y los exporta
  (`forecast:export`) a Excel, PDF, CSV e imagen. La exportación se arma en el navegador con los datos de la
  API; el permiso decide quién ve los botones.
- **Analista:** consulta la hoja, recalcula en vivo y guarda pronósticos; no edita la hoja ni exporta.
- Todos los que consultan ven todas las referencias (respuesta 24 de Industrial) y pueden recalcular sin
  guardar (`POST /forecasts/preview` exige `forecast:read`).

Pendiente con Industrial: umbral de intentos fallidos y procedimiento de recuperación de acceso.
