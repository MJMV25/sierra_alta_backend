-- Regresión lineal como quinto método (Industrial, documento "Error en el proyecto", 2026-11-06).
-- No tiene parámetros: periodos, alfa y beta quedan nulos.
ALTER TABLE metodo_ejecutado DROP CONSTRAINT ck_metodo_ejecutado_codigo;
ALTER TABLE metodo_ejecutado ADD CONSTRAINT ck_metodo_ejecutado_codigo CHECK (metodo IN ('SIMPLE_MOVING_AVERAGE',
    'WEIGHTED_MOVING_AVERAGE', 'EXPONENTIAL_SMOOTHING', 'TREND_ADJUSTED_EXPONENTIAL_SMOOTHING',
    'LINEAR_REGRESSION'));

ALTER TABLE metodo_ejecutado DROP CONSTRAINT ck_metodo_ejecutado_parametros;
ALTER TABLE metodo_ejecutado ADD CONSTRAINT ck_metodo_ejecutado_parametros CHECK (
    (metodo IN ('SIMPLE_MOVING_AVERAGE', 'WEIGHTED_MOVING_AVERAGE')
        AND periodos >= 1 AND alfa IS NULL AND beta IS NULL)
    OR (metodo = 'EXPONENTIAL_SMOOTHING'
        AND periodos IS NULL AND alfa > 0 AND alfa < 1 AND beta IS NULL)
    OR (metodo = 'TREND_ADJUSTED_EXPONENTIAL_SMOOTHING'
        AND periodos IS NULL AND alfa > 0 AND alfa < 1 AND beta > 0 AND beta < 1)
    OR (metodo = 'LINEAR_REGRESSION'
        AND periodos IS NULL AND alfa IS NULL AND beta IS NULL));
