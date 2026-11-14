# ADR 13 — Hoja de datos editable, recálculo en vivo y sin estado de aceptación

**Estado:** aceptado (2026-11-14)

**Contexto.** Ingeniería Industrial envió un mockup de la interfaz (tema oscuro, menú lateral rojo,
indicadores rápidos, gráfico real contra pronóstico y tabla de resultados) y pidió, para la presentación:

- que lo primero al entrar sea una hoja como la hoja DATOS del Excel, con los meses de la empresa, donde se
  puedan corregir o agregar meses;
- que funcione "como el Excel": al cambiar un parámetro (las celdas K0…Kn, α, β, n, L encima de cada
  método) o un dato, se recalcule todo en la misma pantalla, sin cambiar de módulo;
- poder exportar la hoja a Excel, editarla y volver a cargarla;
- quitar el estado *cumple / no cumple*: con los datos de Sierra Alta, muy desbalanceados, casi ningún
  método baja del umbral;
- mostrar el mejor MAD y el mejor MAPE (además de la comparación completa);
- mantener los datos de Spokane y ACME para la presentación, marcados como datos de ejemplo;
- que los pesos K del promedio ponderado sumen exactamente 1 (ya era una regla del ADR 07).

**Alternativas.** Para el recálculo en vivo: (a) copiar las fórmulas a JavaScript y calcular en el
navegador; (b) un endpoint que calcula sin guardar. Para editar datos: (a) mantener la carga solo de meses
nuevos; (b) permitir corregir y borrar con historial.

**Decisión.**
1. **Vista previa sin guardar:** `POST /api/v1/forecasts/preview` hace los mismos cálculos que una
   ejecución y devuelve el resultado con su tabla comparativa, sin guardar nada. Acepta `actuals`
   opcional: los datos de la hoja que aún no se guardan (o una prueba sobre los datos de ejemplo). Las
   fórmulas siguen viviendo solo en Java, verificadas contra el Excel (ADR 07 y 09); el navegador pide un
   cálculo 350 ms después de la última tecla y cancela el anterior. Lo usan todos los roles que consultan
   pronósticos (`forecast:read`).
2. **Hoja editable:** `PATCH /api/v1/history/observations` agrega, corrige y borra meses de una o varias
   series en una transacción. Reglas (`MonthlyEditRules`): unidades enteras ≥ 0, sin meses futuros, sin
   huecos (un mes sin producción se registra con 0) y solo se borran los últimos meses. Un `dataVersion`
   opcional por serie rechaza con 409 el guardado si otro la cambió mientras tanto.
3. **Historial de cambios:** cada guardado es una `carga` (quién y cuándo; su id es la nueva versión de
   datos) y cada mes que cambió queda en `cambio_observacion` con el valor anterior y el nuevo
   (`GET /api/v1/history/series/{id}/changes`). La versión de datos es la última carga de la serie, no la
   carga de sus observaciones, para que avance también al borrar. Los pronósticos guardados no cambian:
   conservan los datos con que se calcularon (tabla `entrada`, ADR 03).
4. **Permisos:** solo el administrador edita la hoja (`history:write`); el analista deja de cargar
   históricos y pasa a guardar pronósticos (`forecast:execute`); el planeador consulta, recalcula y
   exporta. Decisión del equipo con Industrial (2026-11-14).
5. **Sin estado de aceptación:** se eliminan `AcceptanceStatus`, el umbral `MAPE_THRESHOLD_PERCENT` y la
   columna `ejecucion.umbral_mape` (V10). Se siguen mostrando MAD y MAPE con el mejor de cada uno.
6. **Mejor método:** la tabla marca el menor MAD y el menor MAPE por separado; si es el mismo método, la
   pantalla muestra una sola tarjeta "Mejor método". El **estacional multiplicativo no compite**
   (`MethodCode.comparable()`): se ajusta dentro de la muestra (ADR 12), así que su error no es comparable;
   aparece en la tabla con la marca "No compite".
7. **Error por periodo:** cada punto trae `error` = D(t) − P(t) y `absolutePercentError` = |E(t)| / D(t) ×
   100 (`indicators.domain.PeriodError`), para la tabla de resultados y trazabilidad con las columnas de las
   hojas del Excel.
8. **Datos:** V10 vuelve a crear las 4 referencias de la empresa que V7 había borrado y marca Spokane y
   ACME como `producto.ejemplo`. Las de ejemplo no se pueden modificar; en la pantalla se pueden cambiar
   para probar (vista previa con `actuals`), pero nada se guarda.
9. **Excel de ida y vuelta (frontend):** se exporta con el formato de la hoja DATOS (un bloque por año con
   REFERENCIA, ENERO…DICIEMBRE y TOTALES) y la importación reconoce ese formato, venga de la aplicación o
   del Excel original de Industrial. Lo importado entra a la hoja como cambios sin guardar, resaltados,
   para revisarlos antes de "Guardar cambios".

**Consecuencias.**
- (+) Una sola pantalla para datos, parámetros, indicadores, gráfico y tabla, como pidió Industrial.
- (+) No hay dos implementaciones de las fórmulas: la vista previa y el guardado usan el mismo código.
- (+) Cada corrección queda auditada y los pronósticos guardados siguen siendo reproducibles.
- (−) Cada cambio de parámetro es una solicitud al servidor; con series de decenas de meses tarda pocos
  milisegundos (ADR 11), pero depende de la red.
- (−) El endpoint de carga de meses nuevos (`POST /history/series/{id}/observations`) se reemplaza por el
  de la hoja; `docs/datos/cargar-historicos.sh` usa el nuevo.
- Reemplaza la parte del umbral del ADR 07 (pregunta 21) y el alcance del analista del ADR 05.
