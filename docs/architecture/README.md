# Arquitectura del backend

Resumen operativo del documento *Arquitectura del sistema web de pronóstico de producción*
(primera entrega, 18-10-2026). Aquí se registra cómo se materializa en el código.

## Estilo

- **Cliente–servidor**: React (navegador) ↔ API REST JSON de Spring Boot.
- **Monolito modular**: un solo desplegable con los módulos `auth`, `history`, `forecast`, `indicators`
  y una única base PostgreSQL. Los módulos colaboran con llamadas internas, no por red.
- **Multicapa por módulo**: `api` (presentación) → `application` (casos de uso, transacciones) →
  `domain` (reglas y cálculos, sin web ni SQL) ; `infrastructure` implementa persistencia y seguridad.

## Reglas de dependencia entre módulos

```
history  ◄── forecast ──►  indicators         auth protege los accesos a los tres
(series)   (coordina)      (evalúa errores)
```

- `forecast` consulta series solo mediante el servicio público de `history` y entrega pares a `indicators`.
- `indicators` nunca invoca a `forecast` (sin ciclos).
- Un módulo no lee tablas de otro módulo.
- `shared` contiene solo piezas transversales (errores, CORS); no lógica de negocio.

## Seguridad (driver DR03)

| Táctica                         | Implementación |
|---------------------------------|----------------|
| Autenticación central           | `SessionAuthenticator` + `AuthenticationManager` (DAO sobre PostgreSQL) |
| Hash adaptativo                 | `DelegatingPasswordEncoder` (bcrypt, prefijo `{bcrypt}` para migrar) |
| Protección de sesión            | Cookie `HttpOnly` + `SameSite=Lax` (+ `Secure` con HTTPS), cambio de id al autenticar |
| CSRF                            | Cookie `XSRF-TOKEN` + cabecera `X-XSRF-TOKEN`, renovado en el login |
| Autorización en servidor        | Matriz única en `SecurityConfig`, por permiso; denegación por defecto; `@PreAuthorize` en los casos de uso como segunda verificación |
| Privilegio mínimo               | Alcance de cada rol en `RoleCode`; el admin no hereda permisos de negocio |
| Cuentas inactivas               | No pueden autenticarse; `AccountStatusFilter` cierra sus sesiones abiertas |
| Sin enumeración de usuarios     | Mismo 401 para usuario inexistente, clave errónea o cuenta inactiva |
| Auditoría                       | Tabla `evento`: LOGIN, LOGOUT, ACCESS_DENIED, CSRF_REJECTED y cambios de cuentas, sin secretos |
| Privilegio mínimo en la base    | La aplicación usa una cuenta solo con DML; las migraciones, otra dueña del esquema ([ADR 06](adr/0006-cuentas-de-base-de-datos.md)) |

## Datos

Migraciones Flyway en `src/main/resources/db/migration`:

- `V1__identidad_y_auditoria.sql`: `rol`, `usuario`, `evento` + catálogo de los 4 roles.
- `V2__historicos_y_pronosticos.sql`: `producto`, `serie`, `observacion`, `ejecucion`, `entrada`,
  `resultado`, `metrica` con las restricciones de integridad del documento (periodo mensual normalizado,
  valores no negativos, una observación por serie y periodo, instantánea inmutable de entradas, métricas
  con pares usados/excluidos y valor nulo cuando no es calculable).

Los cambios de esquema siempre se hacen con una nueva migración `V3__...`; nunca editando una aplicada.

## Pruebas

- H2 en modo PostgreSQL con las mismas migraciones (sin Docker) para `mvnw test`.
- El CSRF de las pruebas usa doble envío real (cookie + cabecera); no se usa `csrf()` de
  spring-security-test porque sustituye el repositorio de tokens y ocultaría la configuración real.

## Decisiones

- [ADR 01 — Monolito modular multicapa](adr/0001-monolito-modular.md)
- [ADR 02 — Frontend React separado y API REST con sesión](adr/0002-react-y-api-rest.md)
- [ADR 03 — PostgreSQL, Flyway e instantánea por ejecución](adr/0003-postgresql-y-trazabilidad.md)
- [ADR 04 — Facade y Strategy para el pronóstico](adr/0004-facade-y-strategy.md)
- [ADR 05 — Alcance de roles en código y sesión revalidada](adr/0005-alcance-de-roles.md)
- [ADR 06 — Cuentas de base de datos con privilegio mínimo](adr/0006-cuentas-de-base-de-datos.md)
- [ADR 07 — Reglas de cálculo, evaluación y datos acordadas con Industrial](adr/0007-reglas-de-calculo.md)
