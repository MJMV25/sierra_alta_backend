# ADR 11 — Medir el rendimiento antes de optimizar y respaldar la base con restauración de prueba

**Estado:** propuesto

**Contexto.** El Corte 1 propuso dos metas sin compromiso de la empresa: percentil 95 de respuesta ≤ 2 s
con 5 sesiones simultáneas durante 5 minutos y series de hasta 1.000 observaciones (escenario QR1), y un
respaldo con restauración de prueba (escenario QD1). El volumen real es pequeño: cuatro referencias
mensuales y, en esta fase, dos casos de 30 y 25 datos.

**Alternativas.** Agregar desde ahora caché, réplicas o servicios separados · no fijar ninguna táctica ·
medir primero y cambiar la arquitectura solo ante un problema observado.

**Decisión.**
1. Rendimiento: no se agregan cachés, réplicas ni servicios separados. Se usan consultas acotadas
   (historial de ejecuciones limitado a 50, lotes de carga de hasta 240 meses) e índices por las consultas
   reales. La meta QR1 se medirá con una prueba de carga en un equipo documentado antes de cualquier
   optimización.
2. Recuperación: la base PostgreSQL se respalda con las herramientas propias del motor y se hace al menos
   una restauración de prueba en una base vacía, aplicando después las migraciones pendientes. La pérdida
   máxima aceptable (RPO) y el tiempo de recuperación (RTO) se acordarán con la empresa.

**Consecuencias.**
- (+) Se mantiene el monolito simple del [ADR 01](0001-monolito-modular.md); cualquier optimización
  llegará con una medición que la justifique.
- (−) Hasta medir, QR1 es una meta y no un resultado; hasta hacer la restauración, QD1 tampoco está
  comprobado.
- (−) Una caída de la aplicación o de la base afecta a todos los módulos; se acepta por el tamaño del sistema.
