# Sierra Alta — Backend del sistema de pronóstico de producción

API REST del sistema web de pronóstico de producción (Sierra Alta S.A.S. BIC, marca Eleven Pro).
Monolito modular multicapa en **Java 21 + Spring Boot 4.1 + Spring Security 7 + PostgreSQL**.
El frontend React + Vite vive en un repositorio/carpeta aparte y consume esta API.

> Estado actual: **arquitectura completa, inicio de sesión y alcance por rol totalmente funcionales.**
> Los módulos `history`, `forecast` e `indicators` tienen su esquema de base de datos y sus endpoints
> protegidos por rol, que responden `501 Not Implemented` hasta que Ingeniería Industrial valide sus reglas.

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
├── history/                      Productos, series y observaciones   (contrato + permisos)
├── forecast/                     Coordinación de ejecuciones (Facade) (contrato + permisos)
├── indicators/                   MAD y MAPE                           (contrato + permisos)
└── shared/                       Errores RFC 9457 (problem+json), CORS y prefijo /api/v1
src/main/resources
├── application.yml / application-dev.yml
└── db/migration/                 V1 identidad y auditoría · V2 históricos y pronósticos (Flyway)
docker/postgres/initdb/           Cuentas de base de datos con privilegio mínimo
docs/architecture/                Arquitectura, API, matriz de roles y ADR
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

- `mvnw test`: 88 pruebas (alcance por rol en unidad, flujo de login, matriz 4 roles × 10 endpoints,
  acceso anónimo, CSRF con su propio código de error y auditoría, errores de formato en español,
  administración de cuentas, permiso exigido también en el caso de uso, sesión invalidada al desactivar,
  cambio de rol inmediato).
- Ejecución contra PostgreSQL 17 en Docker con cuentas separadas: Flyway valida con la cuenta de
  migraciones, la aplicación opera con la cuenta sin DDL ni `DELETE`; login, `me`, CSRF rechazado,
  ruta sin versión denegada, JSON malformado, logout y eventos de auditoría comprobados con `curl`.
