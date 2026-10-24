# ADR 06 — Cuentas de base de datos con privilegio mínimo

**Estado:** propuesto

**Contexto.** La aplicación se conectaba con el usuario que crea el contenedor de PostgreSQL, que es
superusuario. Una inyección SQL o una credencial filtrada daría control total de la base (regla R49).

**Alternativas.** Una sola cuenta superusuario · una sola cuenta dueña del esquema · cuentas separadas
para migraciones y para la aplicación.

**Decisión.** Tres cuentas, creadas por `docker/postgres/initdb/01-cuentas-aplicacion.sh`:

| Cuenta (`.env`)         | Uso                    | Privilegios |
|-------------------------|------------------------|-------------|
| `POSTGRES_USER`         | Administración manual  | Superusuario; la aplicación no la usa |
| `DB_MIGRATION_USERNAME` | Flyway al arrancar     | Dueña de las tablas (DDL) |
| `DB_USERNAME`           | Aplicación (Hikari)    | `SELECT`, `INSERT`, `UPDATE`; sin DDL ni `DELETE` |

Los privilegios por defecto (`ALTER DEFAULT PRIVILEGES`) extienden esos permisos a las tablas que creen
las migraciones futuras. La aplicación no necesita `DELETE`: las cuentas se desactivan y los eventos de
auditoría son inmutables.

**Consecuencias.**
- (+) La cuenta expuesta a las solicitudes no puede alterar el esquema, borrar datos ni administrar roles.
- (+) El script es idempotente: también migra una base existente y sincroniza contraseñas con `.env`.
- (−) Una funcionalidad que necesite borrar filas debe conceder `DELETE` en una migración, de forma explícita.
- (−) Flyway usa un DataSource propio; en las pruebas (H2) se usa una sola cuenta porque ese DataSource
  aparte cierra la base en memoria.
