# ADR 03 — PostgreSQL con esquema versionado y entradas conservadas por ejecución

**Estado:** propuesto

**Contexto.** Usuarios, series y resultados tienen relaciones e integridad que conservar, y un
resultado debe poder explicarse aunque el histórico se corrija después.

**Alternativas.** Solo archivos · base relacional.

**Decisión.** PostgreSQL con claves, restricciones `CHECK`/`UNIQUE` y una instantánea (`entrada`) por
ejecución. El esquema se versiona con **Flyway**; Hibernate solo lo valida (`ddl-auto: validate`).

**Consecuencias.**
- (+) Consultas consistentes y trazabilidad de cada ejecución.
- (−) Requiere preparar respaldos y una restauración de prueba.
- (−) Las instantáneas consumen almacenamiento adicional; su retención se acordará cuando haya necesidad.
- Las pruebas automáticas usan H2 en modo PostgreSQL con las mismas migraciones; por eso las migraciones
  evitan sintaxis exclusiva de PostgreSQL mientras no sea necesaria.
