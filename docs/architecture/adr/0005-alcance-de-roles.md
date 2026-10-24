# ADR 05 — Alcance de roles definido en código y sesión revalidada en cada solicitud

**Estado:** propuesto

**Contexto.** Hay cuatro roles con operaciones distintas (tabla 2), un rol por usuario y permisos
pendientes de validación con Industrial. El modelo de datos tiene `rol` pero no tablas de permisos.

**Alternativas.** Autorizar directamente por rol en cada endpoint · tablas rol–permiso editables ·
permisos como enum en código asociados a cada rol.

**Decisión.**
1. Cada operación protegida es un `Permission` (`history:read`, `forecast:execute`, …). `RoleCode`
   define qué permisos concede cada rol. Los endpoints se autorizan **por permiso** en una única
   matriz en `SecurityConfig`, con denegación por defecto.
2. `AccountStatusFilter` revalida en cada solicitud la cuenta de la sesión: si está inactiva cierra la
   sesión; si cambió el rol, actualiza las autoridades.

**Consecuencias.**
- (+) Cambiar el alcance de un rol es un cambio localizado (enum + pruebas), sin tocar controladores.
- (+) Desactivar una cuenta o cambiar su rol tiene efecto inmediato.
- (−) Cambiar permisos requiere desplegar; aceptable mientras los roles los defina el proyecto (YAGNI).
- (−) Una consulta por clave primaria adicional por solicitud autenticada; aceptable para el volumen previsto.
