# ADR 09 — Verificar la aplicación con los datos del Excel antes de los datos de la empresa

**Estado:** aceptado (2026-11-06). Reemplaza la regla de periodicidad del [ADR 07](0007-reglas-de-calculo.md)
que dejaba a Spokane y ACME solo como pruebas automatizadas.

**Contexto.** El Acta de Entrega (metodología de transición) pide construir el motor primero con los
datos de prueba y habilitar después los datos reales de Sierra Alta. El 2026-11-06 Industrial pidió que,
para la segunda entrega, la aplicación usara solo los datos del Excel, para comprobar en pantalla que da
lo mismo que la hoja RESUMEN.

**Alternativas.** Mantener los datos de la empresa y verificar solo con pruebas automáticas · cargar los
casos del Excel junto a los de la empresa · reemplazar temporalmente los datos de la empresa por los casos.

**Decisión.**
1. La migración `V7__datos_de_prueba_del_excel.sql` borra los datos de Sierra Alta (ejecuciones, cargas,
   observaciones y las 4 referencias) y crea `CASO-1-SPOKANE` (30 datos, galones) y `CASO-2-ACME`
   (25 datos) como demanda.
2. Nueva periodicidad `PERIODO`: los periodos se numeran t = 1, 2, 3… como en el Excel. Se guardan como
   meses ficticios desde 2000-01 para reutilizar la columna de fecha; la interfaz y las exportaciones
   muestran "t = n".
3. Las dos series son de solo lectura: el backend rechaza cargas en ellas. Su carga figura a nombre de la
   cuenta técnica `sistema`, inactiva y sin contraseña conocida.
4. Cuando el sistema quede verificado, una nueva migración volverá a crear las referencias de la empresa
   y sus datos se cargarán por la API ([docs/datos](../../datos/README.md)).

**Consecuencias.**
- (+) Industrial puede comprobar en la aplicación lo mismo que comprueba `ExcelReferenceTest`;
  `ForecastApiTest` verifica que los casos cargados por la migración dan la hoja RESUMEN.
- (+) Cumple la restricción RN6: no se validan datos reales antes que el benchmark.
- (−) Esta migración usa `DELETE`, que la cuenta de la aplicación no tiene ([ADR 06](0006-cuentas-de-base-de-datos.md)):
  solo puede correrla la cuenta de migraciones.
- (−) Los meses ficticios obligan a formatear el periodo según la periodicidad en toda vista y exportación.
