# Pruebas de Software · Corte 1

| Archivo | Contenido |
|---------|-----------|
| `PS_SierraAlta_C1_PlanMaestro.docx` | Plan maestro de pruebas (E1.1), con el Anexo B para la firma de Industrial |
| `PS_SierraAlta_C1_Catalogo_y_Matriz.xlsx` | Catálogo de 34 casos (Anexo A), matriz de trazabilidad (E1.2), cálculo de referencia y riesgos |
| `ejecutar-pruebas-api.sh` | Casos verificables por la API, contra el backend en marcha |
| `evidencias/` | Resultados de la ejecución del 22/10/2026 |

## Ejecutar

```powershell
.\mvnw.cmd test                                                   # 88 pruebas JUnit 5 + MockMvc (H2)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"       # backend con cuentas de demostración
bash docs/pruebas/ejecutar-pruebas-api.sh                         # pruebas de API en vivo
```

El script lee las contraseñas de `.env` sin mostrarlas. Los casos de históricos, pronóstico e indicadores
salen `BLOQUEADO` mientras esos endpoints respondan 501; cuando se implementen, se escribe el valor que
devuelve el sistema en la hoja `Verificacion` y el veredicto de CP-001 a CP-009 se calcula solo.

Las evidencias no deben contener contraseñas, tokens ni datos reales sin anonimizar (Ley 1581 de 2012).
