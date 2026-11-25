-- Datos de prueba del Excel en lugar de los datos de Sierra Alta (pedido de Ingeniería Industrial,
-- 2026-11-06). La segunda entrega verifica que el sistema dé lo mismo que el Excel, y eso solo se puede
-- comprobar con los casos que ya están calculados allí: CASO 1 SPOKANE y CASO 2 ACME (hoja DATOS de
-- "EXCEL CORREGIDO DEL PROYECTO"). Los datos de producción de la empresa se cargarán de nuevo cuando el
-- sistema esté verificado (requerimiento "sustituir datos de prueba por datos de empresa").

-- 1. Se borra todo lo de Sierra Alta: pronósticos, cargas, observaciones y el catálogo de 4 referencias.
DELETE FROM peso;
DELETE FROM metrica;
DELETE FROM resultado;
DELETE FROM metodo_ejecutado;
DELETE FROM entrada;
DELETE FROM ejecucion;
DELETE FROM observacion;
DELETE FROM carga;
DELETE FROM serie;
DELETE FROM producto;

-- 2. Periodicidad PERIODO: periodos numerados t = 1, 2, 3… como en el Excel (Spokane es semanal y ACME
-- trimestral, pero el Excel los numera con t). Cada t se guarda como un mes ficticio desde enero de 2000
-- (t = 1 -> 2000-01, t = 13 -> 2001-01) para reutilizar la columna DATE; la interfaz muestra "t = n".
ALTER TABLE serie DROP CONSTRAINT ck_serie_periodicidad;
ALTER TABLE serie ADD CONSTRAINT ck_serie_periodicidad CHECK (periodicidad IN ('MENSUAL', 'PERIODO'));

-- 3. Cuenta técnica que figura como autora de la carga (toda carga exige usuario). Queda inactiva y con
-- una contraseña que nadie conoce, así que no puede iniciar sesión.
INSERT INTO usuario (rol_id, nombre_usuario, password_hash, activo)
SELECT id, 'sistema', '{bcrypt}$2a$10$niNeBkjrfl96GcSrUu.hBbCLxQF/X9QAe.iKGfDDCSVncTY2LEdJW', FALSE
FROM rol WHERE codigo = 'ANALYST';

-- 4. Los dos casos de prueba, como demanda.
INSERT INTO producto (codigo, nombre) VALUES
    ('CASO-1-SPOKANE', 'Caso 1 · Spokane (compras de gasolina)'),
    ('CASO-2-ACME', 'Caso 2 · ACME Tool Company');

INSERT INTO serie (producto_id, medida, periodicidad, unidad)
SELECT id, 'DEMANDA', 'PERIODO', CASE codigo WHEN 'CASO-1-SPOKANE' THEN 'GALONES' ELSE 'UNIDADES' END
FROM producto ORDER BY id;

INSERT INTO carga (serie_id, usuario_id, cantidad, periodo_desde, periodo_hasta)
SELECT s.id, u.id, CASE p.codigo WHEN 'CASO-1-SPOKANE' THEN 30 ELSE 25 END, DATE '2000-01-01',
       CASE p.codigo WHEN 'CASO-1-SPOKANE' THEN DATE '2002-06-01' ELSE DATE '2002-01-01' END
FROM serie s
JOIN producto p ON p.id = s.producto_id
JOIN usuario u ON u.nombre_usuario = 'sistema';

-- Valores de la hoja DATOS: Spokane t = 1..30 (galones), ACME t = 1..25 (2000 T1 a 2006 T1).
INSERT INTO observacion (serie_id, periodo, valor, carga_id)
SELECT s.id, v.periodo, v.valor, c.id
FROM (VALUES
    ('CASO-1-SPOKANE', DATE '2000-01-01', 275),
    ('CASO-1-SPOKANE', DATE '2000-02-01', 291),
    ('CASO-1-SPOKANE', DATE '2000-03-01', 307),
    ('CASO-1-SPOKANE', DATE '2000-04-01', 281),
    ('CASO-1-SPOKANE', DATE '2000-05-01', 295),
    ('CASO-1-SPOKANE', DATE '2000-06-01', 268),
    ('CASO-1-SPOKANE', DATE '2000-07-01', 252),
    ('CASO-1-SPOKANE', DATE '2000-08-01', 279),
    ('CASO-1-SPOKANE', DATE '2000-09-01', 264),
    ('CASO-1-SPOKANE', DATE '2000-10-01', 288),
    ('CASO-1-SPOKANE', DATE '2000-11-01', 302),
    ('CASO-1-SPOKANE', DATE '2000-12-01', 287),
    ('CASO-1-SPOKANE', DATE '2001-01-01', 290),
    ('CASO-1-SPOKANE', DATE '2001-02-01', 311),
    ('CASO-1-SPOKANE', DATE '2001-03-01', 277),
    ('CASO-1-SPOKANE', DATE '2001-04-01', 245),
    ('CASO-1-SPOKANE', DATE '2001-05-01', 282),
    ('CASO-1-SPOKANE', DATE '2001-06-01', 277),
    ('CASO-1-SPOKANE', DATE '2001-07-01', 298),
    ('CASO-1-SPOKANE', DATE '2001-08-01', 303),
    ('CASO-1-SPOKANE', DATE '2001-09-01', 310),
    ('CASO-1-SPOKANE', DATE '2001-10-01', 299),
    ('CASO-1-SPOKANE', DATE '2001-11-01', 285),
    ('CASO-1-SPOKANE', DATE '2001-12-01', 250),
    ('CASO-1-SPOKANE', DATE '2002-01-01', 260),
    ('CASO-1-SPOKANE', DATE '2002-02-01', 245),
    ('CASO-1-SPOKANE', DATE '2002-03-01', 271),
    ('CASO-1-SPOKANE', DATE '2002-04-01', 282),
    ('CASO-1-SPOKANE', DATE '2002-05-01', 302),
    ('CASO-1-SPOKANE', DATE '2002-06-01', 285),
    ('CASO-2-ACME', DATE '2000-01-01', 500),
    ('CASO-2-ACME', DATE '2000-02-01', 350),
    ('CASO-2-ACME', DATE '2000-03-01', 250),
    ('CASO-2-ACME', DATE '2000-04-01', 400),
    ('CASO-2-ACME', DATE '2000-05-01', 450),
    ('CASO-2-ACME', DATE '2000-06-01', 350),
    ('CASO-2-ACME', DATE '2000-07-01', 200),
    ('CASO-2-ACME', DATE '2000-08-01', 300),
    ('CASO-2-ACME', DATE '2000-09-01', 350),
    ('CASO-2-ACME', DATE '2000-10-01', 200),
    ('CASO-2-ACME', DATE '2000-11-01', 150),
    ('CASO-2-ACME', DATE '2000-12-01', 400),
    ('CASO-2-ACME', DATE '2001-01-01', 550),
    ('CASO-2-ACME', DATE '2001-02-01', 350),
    ('CASO-2-ACME', DATE '2001-03-01', 250),
    ('CASO-2-ACME', DATE '2001-04-01', 550),
    ('CASO-2-ACME', DATE '2001-05-01', 550),
    ('CASO-2-ACME', DATE '2001-06-01', 400),
    ('CASO-2-ACME', DATE '2001-07-01', 350),
    ('CASO-2-ACME', DATE '2001-08-01', 600),
    ('CASO-2-ACME', DATE '2001-09-01', 750),
    ('CASO-2-ACME', DATE '2001-10-01', 500),
    ('CASO-2-ACME', DATE '2001-11-01', 400),
    ('CASO-2-ACME', DATE '2001-12-01', 650),
    ('CASO-2-ACME', DATE '2002-01-01', 850)
) AS v (codigo, periodo, valor)
JOIN producto p ON p.codigo = v.codigo
JOIN serie s ON s.producto_id = p.id
JOIN carga c ON c.serie_id = s.id;
