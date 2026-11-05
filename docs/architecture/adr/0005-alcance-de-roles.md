# ADR 05 — Alcance de roles definido en código y sesión revalidada en cada solicitud

**Estado:** propuesto

**Contexto.** Había cuatro roles con operaciones distintas (tabla 2; tres desde el ajuste del final), un
rol por usuario y permisos pendientes de validación con Industrial. El modelo de datos tiene `rol` pero no tablas de permisos.

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

## Ajuste del 2026-11-05: tres roles y pronósticos solo para el administrador

**Contexto.** El equipo revisó el alcance de la tabla 2: el planeador es quien extrae la información de
los pronósticos, no quien los calcula; el supervisor de línea duplicaba funciones del analista.

**Decisión.**
- `ADMIN` recibe todos los permisos y es el único con `forecast:execute`.
- `PLANNER` consulta históricos, pronósticos e indicadores; la exportación, cuando exista, será suya y del
  administrador.
- `ANALYST` carga y consulta históricos y consulta pronósticos e indicadores; ya no ejecuta pronósticos.
- Se elimina `SUPERVISOR` (migración V6). Sus cuentas no se borran, porque los eventos de seguridad las
  referencian: pasan a `ANALYST` y quedan inactivas hasta que el administrador decida activarlas.

**Consecuencias.**
- (+) Un solo responsable de los cálculos que quedan guardados.
- (−) El administrador acumula administración de cuentas y operación; conviene revisarlo si llega a haber
  más de un administrador o auditorías de separación de funciones.
