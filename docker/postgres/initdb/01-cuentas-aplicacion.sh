#!/bin/sh
# Cuentas de base de datos con privilegio mínimo (regla R49).
#
#   POSTGRES_USER          superusuario del contenedor; solo administración, nunca la aplicación.
#   DB_MIGRATION_USERNAME  dueña de las tablas; Flyway la usa para crear y alterar el esquema (DDL).
#   DB_APP_USERNAME        cuenta de la aplicación: SELECT, INSERT y UPDATE; sin DDL ni DELETE
#                          (las cuentas se desactivan y la auditoría es inmutable).
#
# Postgres lo ejecuta solo al crear el volumen. Es idempotente: para una base existente ejecútelo con
#   docker compose exec db sh /docker-entrypoint-initdb.d/01-cuentas-aplicacion.sh
# Si la base ya tenía tablas de otro dueño, se transfieren a la cuenta de migraciones.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
	-v db="$POSTGRES_DB" \
	-v migrator="$DB_MIGRATION_USERNAME" -v migrator_password="$DB_MIGRATION_PASSWORD" \
	-v app="$DB_APP_USERNAME" -v app_password="$DB_APP_PASSWORD" <<'SQL'
-- Crea las cuentas si faltan y sincroniza sus contraseñas con el .env.
SELECT format('CREATE ROLE %I LOGIN', :'migrator')
	WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'migrator') \gexec
SELECT format('CREATE ROLE %I LOGIN', :'app')
	WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app') \gexec
SELECT format('ALTER ROLE %I WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L',
	:'migrator', :'migrator_password') \gexec
SELECT format('ALTER ROLE %I WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L',
	:'app', :'app_password') \gexec

GRANT CONNECT ON DATABASE :"db" TO :"migrator", :"app";
GRANT USAGE, CREATE ON SCHEMA public TO :"migrator";
GRANT USAGE ON SCHEMA public TO :"app";

-- Tablas creadas antes de separar las cuentas: pasan a la cuenta de migraciones (con sus secuencias).
SELECT format('ALTER TABLE public.%I OWNER TO %I', tablename, :'migrator')
	FROM pg_tables WHERE schemaname = 'public' AND tableowner <> :'migrator' \gexec

-- Permisos sobre lo existente y sobre lo que creen las migraciones futuras.
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO :"app";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO :"app";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator" IN SCHEMA public
	GRANT SELECT, INSERT, UPDATE ON TABLES TO :"app";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator" IN SCHEMA public
	GRANT USAGE, SELECT ON SEQUENCES TO :"app";
SQL
