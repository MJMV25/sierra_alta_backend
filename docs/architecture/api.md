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

### history, forecast, indicators (contrato y permisos; responden 501)

| Método | Ruta                                       | Permiso             |
|--------|--------------------------------------------|---------------------|
| GET    | `/api/v1/history/products`                    | `history:read`      |
| GET    | `/api/v1/history/series/{id}/observations`    | `history:read`      |
| POST   | `/api/v1/history/series/{id}/observations`    | `history:write`     |
| POST   | `/api/v1/forecasts`                           | `forecast:execute`  |
| GET    | `/api/v1/forecasts`                           | `forecast:read`     |
| GET    | `/api/v1/forecasts/{id}`                      | `forecast:read`     |
| GET    | `/api/v1/indicators/executions/{id}`          | `indicators:read`   |

Cualquier ruta no declarada en `SecurityConfig` se deniega (403 con sesión, 401 sin ella).

## Matriz de alcance por rol

Definida en `RoleCode` (tabla 2 del documento de arquitectura) y verificada por `RoleScopeTest`.

| Permiso            | ADMIN | PLANNER | SUPERVISOR | ANALYST |
|--------------------|:-----:|:-------:|:----------:|:-------:|
| `users:manage`     |  ✔    |         |            |         |
| `history:read`     |       |   ✔     |    ✔       |   ✔     |
| `history:write`    |       |         |            |   ✔     |
| `forecast:execute` |       |   ✔     |            |   ✔     |
| `forecast:read`    |       |   ✔     |    ✔       |   ✔     |
| `indicators:read`  |       |   ✔     |    ✔       |   ✔     |

El administrador **no** obtiene automáticamente permisos sobre históricos ni pronósticos.
Pendiente con Industrial: filtro por línea del supervisor, umbral de intentos fallidos y
procedimiento de recuperación de acceso.
