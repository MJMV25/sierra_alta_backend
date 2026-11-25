# ADR 12 — Estacional multiplicativo como sexto método, idéntico al Excel

**Estado:** aceptado (2026-11-13)

**Contexto.** El archivo `EXCEL - ENTREGA 2.xlsx` trae las hojas *CASO 1 ESTACIONAL MULTIPLI.* y
*CASO 2 ESTACIONAL MULTIPLI.*, y las enlaza en el RESUMEN (MAD 13,0333 y 31,4167; MAPE 4,68 % y 8,88 %).
Industrial definió el Excel como el estándar de calibración y el equipo decidió reproducir el método tal
como está en esas hojas. El método tiene rasgos que no comparte con los otros cinco:

- cada periodo de un ciclo se "pronostica" con el promedio de **su propio ciclo**, que incluye ese dato y
  los posteriores (ajuste dentro de la muestra, no pronóstico hacia adelante);
- redondea los pronósticos a entero (`REDONDEAR(…; 0)`);
- su MAD y su MAPE promedian **todos** los periodos de la hoja;
- la hoja de ACME usa 24 trimestres (2000-2005); el trimestre 25 queda fuera.

**Alternativas.** Reproducir las hojas tal como están · una versión que solo use datos pasados (no
coincide con el Excel) · dejar el método fuera del alcance.

**Decisión.**
1. `SeasonalMultiplicative` implementa `ForecastMethod` (Strategy del [ADR 04](0004-facade-y-strategy.md))
   con las mismas fórmulas de las celdas, en el mismo orden:
   - promedio de cada ciclo completo `c` = `PROMEDIO(ciclo c)`;
   - índice de la posición `p` = promedio, sobre los ciclos completos, de `D(c, p) / promedio(c)`;
   - pronóstico de un periodo de un ciclo completo = `REDONDEAR(promedio(c) × índice(p); 0)`;
   - ciclo proyectado: promedio = (total del último ciclo + promedio de los incrementos entre totales
     consecutivos) / L; pronóstico = `REDONDEAR(promedio proyectado × índice(p); 0)`.
2. **Parámetro L** (`seasonLength`, columna `longitud_ciclo`): periodos por ciclo. Entero ≥ 2 y
   ≤ 120. El Excel usa L = 5 (Spokane) y L = 4 (ACME); en una serie mensual corresponde L = 12.
3. **Ciclos:** se cuentan desde el primer dato del periodo elegido. Se necesitan al menos **2 ciclos
   completos**: con uno solo no hay incremento para proyectar el siguiente. Un ciclo con promedio 0 se
   informa como error de la serie en lugar de dividir por cero.
4. **Ciclo incompleto al final:** sus periodos toman el ciclo proyectado, igual que las celdas de la
   columna I del Excel (en ACME con 25 trimestres, 2006 T1 = 794). No se recorta la serie en silencio.
5. **Horizonte** (respuesta 12): solo el periodo siguiente al último dato, tomado del ciclo proyectado
   (Spokane: celda I3 = 267; ACME con 24 trimestres: celda I4 = 794).
6. **Redondeo:** como `REDONDEAR` de Excel, mitades lejos de cero y a 15 cifras significativas antes de
   redondear, para que un 282,5 calculado como 282,49999999999997 también suba.
7. **Evaluación:** da pronóstico desde t = 1, así que **no recorta la ventana común** (como la regresión,
   [ADR 08](0008-regresion-lineal.md)). Ejecutado solo, se evalúa en todos los periodos y reproduce el
   RESUMEN; junto a otros métodos se evalúa en la ventana común, como todos.
8. La migración `V9__estacional_multiplicativo.sql` agrega `longitud_ciclo` y amplía los `CHECK` de
   `metodo_ejecutado`: solo el estacional la usa y los demás la dejan nula. Una ejecución admite hasta
   6 métodos.

**Consecuencias.**
- (+) `SeasonalMultiplicativeTest` compara cada pronóstico, el ciclo proyectado y MAD y MAPE con las
  hojas (casos dorados `seasonal-spokane.csv` y `seasonal-acme.csv`, generados del Excel) con la tolerancia
  del ADR 07. `ForecastApiTest` repite los dos casos por la API.
- (+) Confirma de nuevo el escenario QM1: una clase, una migración y sus pruebas, sin tocar los otros
  métodos.
- (−) Por el ajuste dentro de la muestra, su MAD sale menor que el de los demás y **no es comparable** con
  ellos: los otros pronostican cada periodo solo con datos anteriores. La pantalla lo advierte.
- (−) Para que el RESUMEN de ACME coincida hay que elegir el periodo t = 1..24 (hasta 2001-12 en la
  aplicación); con los 25 trimestres el último toma el ciclo proyectado y el MAD cambia.
