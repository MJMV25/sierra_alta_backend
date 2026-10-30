# Sierra Alta — Backend del sistema de pronóstico de producción

API REST del sistema web de pronóstico de producción (Sierra Alta S.A.S. BIC, marca Eleven Pro).
Monolito modular multicapa en **Java 21 + Spring Boot 4.1 + Spring Security 7 + PostgreSQL**.
El frontend React + Vite vive en un repositorio/carpeta aparte y consume esta API.

> Estado actual: **inicio de sesión, alcance por rol e históricos (fase 1) totalmente funcionales.**
> Los cuatro métodos de pronóstico y los indicadores MAD/MAPE están implementados como dominio y
> verificados contra el Excel corregido; los endpoints de `forecast` e `indicators` responden
> `501 Not Implemented` hasta conectarlos con su caso de uso.

## Estructura

```
src/main/java/co/sierraalta/production
├── SierraAltaBackendApplication.java
├── auth/                         Identidad, sesión, roles, permisos y auditoría  ← funcional
│   ├── api/                      AuthController, UserAdministrationController, dto/
│   ├── application/              UserAdministrationService, SecurityEventRecorder
│   ├── domain/                   Role, RoleCode (alcance por rol), Permission, UserAccount, SecurityEvent
│   └── infrastructure/
│       ├── security/             SecurityConfig (matriz de autorización), sesión, CSRF, filtros
│       ├── persistence/          Repositorios Spring Data
│       └── bootstrap/            Creación del primer administrador y cuentas demo
├── history/                      Productos, series, observaciones y cargas  ← funcional
├── forecast/                     4 métodos en domain/ (Strategy); caso de uso y endpoints pendientes
├── indicators/                   MAD y MAPE en domain/; endpoint pendiente
└── shared/                       Errores RFC 9457 (problem+json), CORS y prefijo /api/v1
src/main/resources
├── application.yml / application-dev.yml
└── db/migration/                 V1 identidad y auditoría · V2 históricos y pronósticos ·
                                  V3 cargas y catálogo de referencias (Flyway)
src/test/resources/forecast/golden/  Casos Spokane y ACME copiados del Excel corregido
docker/postgres/initdb/           Cuentas de base de datos con privilegio mínimo
docs/architecture/                Arquitectura, API, matriz de roles y ADR
docs/datos/                       Carga de los históricos reales por la API (no se versionan)
.github/workflows/ci.yml          Compilación con lint y pruebas en cada push / pull request
```

Cada módulo separa presentación (`api`), aplicación, dominio e infraestructura solo cuando tiene esa
responsabilidad. Detalle en [docs/architecture/README.md](docs/architecture/README.md).

## Requisitos

- JDK 21 o superior (el proyecto compila con `release 21`).
- Docker Desktop (para PostgreSQL local). Maven **no** es necesario: se usa el Maven Wrapper.

## Puesta en marcha

```powershell
# 1. Configuración local (no se versiona)
copy .env.example .env          # y cambie las contraseñas

# 2. Base de datos (al crear el volumen también crea las cuentas de migración y de aplicación)
docker compose up -d db

# 3. Pruebas (usan H2 en memoria; no requieren Docker)
.\mvnw.cmd test

# 4. Ejecutar con cuentas de demostración (perfil dev)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

La aplicación lee el archivo `.env` de la raíz automáticamente (también desde IntelliJ). Flyway crea
el esquema al arrancar y Hibernate solo lo valida (`ddl-auto: validate`). El puerto se cambia con
`SERVER_PORT` (por defecto 8080).

### Variables de entorno

| Variable | Uso |
|----------|-----|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | Contenedor; `POSTGRES_USER` es superusuario y la aplicación no lo usa |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Cuenta de la aplicación: solo `SELECT`/`INSERT`/`UPDATE` |
| `DB_MIGRATION_USERNAME`, `DB_MIGRATION_PASSWORD` | Cuenta dueña del esquema, usada solo por Flyway |
| `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD` | Primer administrador |
| `DEMO_USERS_PASSWORD` | Cuentas de demostración (perfil `dev`) |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend, separados por coma |
| `SESSION_COOKIE_SECURE` | `true` cuando se sirve por HTTPS |
| `SERVER_PORT` | Puerto HTTP del backend |

Si el volumen de la base ya existía antes de separar las cuentas (o cambia sus contraseñas en `.env`),
recree el contenedor y aplique el script, que es idempotente:

```powershell
docker compose up -d db
docker compose exec -T db sh /docker-entrypoint-initdb.d/01-cuentas-aplicacion.sh
```

Detalle de la decisión en [ADR 06](docs/architecture/adr/0006-cuentas-de-base-de-datos.md).

### Históricos de producción

Las cantidades reales de la empresa no están en el repositorio. Tras arrancar el backend, cárguelas por
la API con `bash docs/datos/cargar-historicos.sh` a partir de un CSV local en `datos-locales/`
(ignorado por git). Detalle en [docs/datos/README.md](docs/datos/README.md).

Cuentas iniciales:

| Usuario      | Rol        | Contraseña                    | Cuándo se crea                           |
|--------------|------------|-------------------------------|------------------------------------------|
| `admin`*     | ADMIN      | `BOOTSTRAP_ADMIN_PASSWORD`    | Si no existe ningún administrador activo |
| `planeador`  | PLANNER    | `DEMO_USERS_PASSWORD`         | Solo con el perfil `dev`                 |
| `supervisor` | SUPERVISOR | `DEMO_USERS_PASSWORD`         | Solo con el perfil `dev`                 |
| `analista`   | ANALYST    | `DEMO_USERS_PASSWORD`         | Solo con el perfil `dev`                 |

\* nombre configurable con `BOOTSTRAP_ADMIN_USERNAME`.

## Probar el login a mano

```bash
curl -c jar http://localhost:8080/api/v1/auth/csrf                      # obtiene cookie XSRF-TOKEN
TOKEN=$(grep XSRF-TOKEN jar | awk '{print $7}')
curl -b jar -c jar -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json" \
     -d '{"username":"planeador","password":"<DEMO_USERS_PASSWORD>"}' \
     http://localhost:8080/api/v1/auth/login
TOKEN=$(grep XSRF-TOKEN jar | awk '{print $7}')                      # el login renueva el token
curl -b jar http://localhost:8080/api/v1/auth/me
```

Contrato completo de la API y matriz de permisos: [docs/architecture/api.md](docs/architecture/api.md).

## Calidad

- `mvnw verify` compila con `-Xlint:all` (advertencias de javac visibles) y ejecuta todas las pruebas;
  es lo mismo que corre la integración continua (`.github/workflows/ci.yml`).
- `.editorconfig` fija codificación, finales de línea e indentación para cualquier editor.
- Reglas de desarrollo del proyecto: [PROJECT_RULES.md](PROJECT_RULES.md).

## Verificación realizada

- `mvnw verify` (29-10-2026): 129 pruebas, sin advertencias de `-Xlint:all`. Además de las de
  autenticación y roles (alcance por rol, login, matriz 4 roles × 10 endpoints, CSRF, auditoría,
  administración de cuentas, permiso exigido también en el caso de uso), incluye:
  - los 4 métodos contra cada periodo y los MAD/MAPE de las hojas Spokane y ACME (6 decimales);
  - reglas de parámetros y del MAPE con reales en cero;
  - carga de históricos: orden, versión de datos, conflicto por mes repetido (409), huecos y meses
    futuros (422), errores por campo (400), lote rechazado sin efectos, permiso en el caso de uso.
- Ejecución contra PostgreSQL 17 en Docker con cuentas separadas: Flyway valida con la cuenta de
  migraciones, la aplicación opera con la cuenta sin DDL ni `DELETE`; login, `me`, CSRF rechazado,
  ruta sin versión denegada, JSON malformado, logout y eventos de auditoría comprobados con `curl`.
- Fase 1 contra PostgreSQL 17 (29-10-2026): Flyway aplicó V3 con la cuenta de migraciones e Hibernate
  validó las entidades; el script cargó las 4 referencias (57 meses cada una) y una segunda ejecución
  quedó rechazada con 409 sin duplicar datos; los totales coinciden con la hoja DATOS; la cuenta de la
  aplicación no puede borrar observaciones; `docs/pruebas/ejecutar-pruebas-api.sh`: 25 aprobadas,
  0 fallidas, 2 bloqueadas (pronóstico e indicadores).
