# ADR 08 — Agregar la regresión lineal como quinto método

**Estado:** aceptado (2026-11-06)

**Contexto.** El Excel corregido (2) trae una hoja REGRESION LINEAL que no estaba en el RESUMEN.
Industrial la pidió en el documento *Error en el proyecto* (2026-11-06) y señaló dos errores del Excel:
los argumentos de `INTERSECCION.EJE` estaban invertidos y P(t) se redondeaba. Los valores correctos son
intercepto 284,84 (Spokane) y 251,5 (ACME); MAD 15,90 y 118,40; MAPE 5,79 % y 35,94 %.

**Alternativas.** Reproducir el Excel tal como está, con el error · corregir la fórmula según Industrial
· dejar la regresión fuera del alcance.

**Decisión.**
1. `LinearRegression` implementa `ForecastMethod` (Strategy del [ADR 04](0004-facade-y-strategy.md)):
   P(t) = b·t + a con mínimos cuadrados sobre t = 1..N, sin redondear P(t). El pronóstico siguiente es
   b·(N + 1) + a. No tiene parámetros y necesita al menos 2 datos.
2. La recta da un pronóstico desde t = 1, así que no recorta la ventana común de evaluación.
3. La migración `V8__regresion_lineal.sql` amplía los `CHECK` de `metodo_ejecutado` con
   `LINEAR_REGRESSION` y sus parámetros nulos. Una ejecución admite hasta 5 métodos.
4. Los valores de referencia son los corregidos por Industrial, no los del Excel (2).

**Consecuencias.**
- (+) Confirma el escenario de mantenibilidad QM1: el método se agregó con una clase, una migración y sus
  pruebas, sin cambiar los otros cuatro métodos ni `auth`.
- (+) `LinearRegressionTest` fija la pendiente y el intercepto corregido con la tolerancia del ADR 07.
- (−) La regresión ya no coincide con la hoja del Excel (2) mientras Industrial no la corrija; la
  referencia de verificación es su documento, no esa hoja.
- El método estacional multiplicativo se agregó después, idéntico al Excel de la entrega 2 ([ADR 12](0012-estacional-multiplicativo.md)).
