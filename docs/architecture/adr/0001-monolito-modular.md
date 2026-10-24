# ADR 01 — Adoptar un monolito modular multicapa

**Estado:** propuesto

**Contexto.** Equipo pequeño, plazo académico y requisitos de negocio aún en validación con Industrial.

**Alternativas.** Monolito sin límites internos · monolito modular · microservicios.

**Decisión.** Una aplicación Spring Boot con cuatro módulos (`auth`, `history`, `forecast`,
`indicators`) organizados por capas; las llamadas entre módulos son internas.

**Consecuencias.**
- (+) Depuración sencilla, transacciones locales, una sola operación de despliegue.
- (−) Todos los módulos se despliegan juntos y comparten el impacto de una caída.
- (−) Mantener los límites exige revisión: ningún módulo accede a las tablas de otro.
- No se adoptan microservicios sin una necesidad demostrada de despliegue o escalado independiente.
