# Pruebas del backend

| Archivo | Contenido |
|---------|-----------|
| `ejecutar-pruebas-api.sh` | Casos verificables por la API, contra el backend en marcha |
| `evidencias/` | Resultados históricos de ejecución; consultar la fecha de cada archivo |

## Ejecutar

```powershell
.\mvnw.cmd verify                                             # pruebas con H2 y reporte de cobertura
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"       # backend con cuentas de demostración
bash docs/pruebas/ejecutar-pruebas-api.sh                         # pruebas de API en vivo
```

Se requiere JDK 21 o superior. Las pruebas automáticas usan H2 y no requieren Docker ni el backend
en marcha. `verify` genera el reporte de cobertura en `target/site/jacoco/index.html`.

El script de API requiere PostgreSQL, el backend con perfil `dev` y los históricos cargados siguiendo
[la guía de datos](../datos/README.md). Lee las contraseñas de `.env` sin mostrarlas y guarda una nueva
ejecución de pronóstico en cada corrida. Comprueba autenticación, permisos, CSRF, históricos,
pronósticos e indicadores: CP-032 espera una tabla comparativa con umbral y estado, y un 404 para una
ejecución inexistente. Una respuesta 501 en estos casos es un fallo.

Para el catálogo de casos externo, se escribe el valor que devuelve el sistema en la hoja
`Verificacion`; el veredicto de CP-001 a CP-009 se calcula en ese archivo. Ese catálogo no está
versionado en este repositorio.

Las evidencias no deben contener contraseñas, tokens ni datos reales sin anonimizar (Ley 1581 de 2012).
