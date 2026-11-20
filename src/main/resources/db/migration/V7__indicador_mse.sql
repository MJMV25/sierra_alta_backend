-- MSE usa todos los pares de la ventana común, incluidos los reales cero.
-- Las ejecuciones históricas conservan MSE ausente (no se inventa un valor).
ALTER TABLE metrica DROP CONSTRAINT ck_metrica_codigo;
ALTER TABLE metrica ADD CONSTRAINT ck_metrica_codigo CHECK (codigo IN ('MAD', 'MAPE', 'MSE'));
-- El cuadrado del error necesita más dígitos enteros que MAD.
ALTER TABLE metrica ALTER COLUMN valor TYPE NUMERIC(38, 10);
