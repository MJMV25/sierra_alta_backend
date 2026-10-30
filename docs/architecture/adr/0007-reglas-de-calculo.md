# ADR 07 — Reglas de cálculo, evaluación y datos acordadas con Industrial

**Estado:** aceptado, con cuatro puntos por confirmar (ver al final)

**Contexto.** El Acta de Entrega compromete al sistema a reproducir "exactamente" el ANEXO A. El
equipo de software envió 24 preguntas a Ingeniería Industrial (documento *Preguntas para Ingeniería
Industrial – Módulo de pronósticos*, 28-10-2026), que respondió el 29-10-2026 y entregó el archivo
`EXCEL CORREGIDO DEL PROYECTO.xlsx`. Este ADR registra las reglas resultantes.

## Referencia de cálculo

- **Excel corregido** (respuestas 1, 3, 4 y 5): la hoja ACME quedó con sus 25 trimestres reales, el
  RESUMEN enlazado con fórmulas y los gráficos apuntan al mismo archivo. Con esa hoja, el promedio
  móvil ponderado es el mejor modelo de ACME por MAD (106,90) y por MAPE (27,27 %).
- **Exactitud** (6): un valor del sistema "coincide" con el Excel si la diferencia es ≤ 0,000001
  (6 decimales). En pantalla se muestran 2 decimales. Se calcula con `double`, como Excel.
- **Casos dorados:** `src/test/resources/forecast/golden/{spokane,acme}.csv` (valores copiados del Excel
  corregido) y `ExcelReferenceTest`. Spokane: MAD 20,584 · 19,80 · 16,2523 · 16,0608.
  ACME: MAD 116,6667 · 106,9048 · 147,9510 · 132,3801.

## Métodos y parámetros

| Regla | Decisión |
|-------|----------|
| Valores iniciales (11) | Fijos, como en el Excel: suavizamiento F(1) = D(1); Holt A(1) = D(1), T(1) = 0. El primer pronóstico de ambos es el del periodo 2. |
| α y β (10) | Solo estrictamente entre 0 y 1. |
| Pesos del ponderado | Cada uno en (0, 1] y suma igual a 1 (margen 10⁻⁹ por la representación binaria). El primer peso (K0) se aplica al dato más reciente, como en las fórmulas del Excel. |
| n del promedio móvil | Entero ≥ 1 y ≤ datos disponibles. |
| Horizonte (12) | Todos los métodos pronostican solo el periodo siguiente. |
| MSE (9) | Fuera del proyecto: no se calcula ni se muestra. |

## Evaluación

- **Ventana común** (7): al comparar métodos, todos se evalúan desde el primer periodo en que todos
  tienen pronóstico (en Spokane, el 6; en ACME, el 5), como en el Excel. Industrial aclaró que no es una
  regla teórica sino del caso; se adopta porque es la única que reproduce los indicadores del Excel y
  compara a todos los métodos sobre los mismos datos.
- **MAPE con real 0** (8): los periodos con real exactamente 0 se excluyen del MAPE (no del MAD) y se
  informa cuántos; un real pequeño distinto de 0 (p. ej. 0,5) sí cuenta. Si todos son 0, el MAPE es
  "no calculable". Se guarda como fracción y se muestra ×100.
- **Mejor modelo** (22): ni el MAD ni el MAPE mandan. El sistema muestra ambos indicadores para cada
  método y señala el mejor por cada uno, sin elegir un ganador único.
- **Umbral** (21): mientras no se acuerde con la empresa, el sistema muestra "sin criterio definido".
  Cuando se acuerde, aplicará a MAD y a MAPE.

## Datos de Sierra Alta

- **Guion "-"** (14, 15): significa 0 unidades producidas; el Excel corregido ya lo reemplazó por 0.
  El sistema sigue distinguiendo "sin fila" (dato no cargado) de un 0 cargado.
- **Valores atípicos** (16): son pedidos reales (producción bajo pedido). El sistema no los modifica.
- **Medida** (17): se pronostica la producción como demanda, y la interfaz debe decir "producción".
- **Periodicidad** (18): solo mensual para la empresa. Spokane (semanal) y ACME (trimestral) existen solo
  como casos de prueba automatizados, no como series de la base de datos.
- **Carga** (19): el analista podrá agregar los datos de cada mes nuevo.
- **Validación** (20): ambos equipos validan el motor con Spokane y ACME y firman las actas.
- **Supervisores** (24): ven todas las referencias, en solo lectura.
- **Exportación** (23): datos reales y pronosticados, periodo, referencia, método, parámetros,
  indicadores y rango de evaluación, gráfico, fecha y hora de cálculo, usuario, versión de datos y
  advertencias de faltantes o indicadores no calculables.

## Por confirmar con Industrial

Industrial dejó estas preguntas "para resolver mañana". El código sigue la opción indicada; cambiarla es
localizado.

| Pregunta | Opción implementada | Qué cambiaría |
|----------|---------------------|---------------|
| 2. Orden de los pesos en ACME | Pesos 0,1 · 0,2 · 0,3 · 0,4 como en el Excel (0,1 al dato más reciente) | Solo el dato de prueba de ACME y sus valores esperados |
| 12. Horizonte | Solo el periodo siguiente | `Forecast.next` pasaría a una lista |
| 13. n igual al total de datos | Se permite: solo pronóstico siguiente, MAD y MAPE "no calculables" | `MovingWindows.requireEnoughData` |
| 21. Umbral | "Sin criterio definido"; luego MAD y MAPE | Configuración del umbral (general o por referencia) |

**Consecuencias.**
- (+) Las reglas que afectan los números quedan fijadas por pruebas automáticas contra el Excel.
- (−) La ventana común hace que el MAD de un método dependa de con cuáles se compara; el sistema debe
  mostrar siempre el rango evaluado junto a los indicadores.
