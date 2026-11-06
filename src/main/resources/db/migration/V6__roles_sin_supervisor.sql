-- Se elimina el rol Supervisor de línea (decisión del equipo, 2026-11-05, ADR 05): el analista ya cumple
-- sus funciones. Las cuentas que lo tenían no se borran, porque los eventos de seguridad las referencian
-- (auditoría): pasan a Analista y quedan inactivas, así que no pueden iniciar sesión hasta que el
-- administrador decida activarlas.
UPDATE usuario
SET rol_id = (SELECT id FROM rol WHERE codigo = 'ANALYST'),
    activo = FALSE
WHERE rol_id = (SELECT id FROM rol WHERE codigo = 'SUPERVISOR');

DELETE FROM rol WHERE codigo = 'SUPERVISOR';
