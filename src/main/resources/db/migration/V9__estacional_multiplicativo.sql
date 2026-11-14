-- Estacional multiplicativo como sexto método (Excel de la entrega 2, ADR 12).
-- Su único parámetro es L, la longitud del ciclo; los demás métodos lo dejan nulo.
ALTER TABLE metodo_ejecutado ADD COLUMN longitud_ciclo INTEGER;

ALTER TABLE metodo_ejecutado DROP CONSTRAINT ck_metodo_ejecutado_codigo;
ALTER TABLE metodo_ejecutado ADD CONSTRAINT ck_metodo_ejecutado_codigo CHECK (metodo IN ('SIMPLE_MOVING_AVERAGE',
    'WEIGHTED_MOVING_AVERAGE', 'EXPONENTIAL_SMOOTHING', 'TREND_ADJUSTED_EXPONENTIAL_SMOOTHING',
    'LINEAR_REGRESSION', 'SEASONAL_MULTIPLICATIVE'));

ALTER TABLE metodo_ejecutado DROP CONSTRAINT ck_metodo_ejecutado_parametros;
ALTER TABLE metodo_ejecutado ADD CONSTRAINT ck_metodo_ejecutado_parametros CHECK (
    (metodo IN ('SIMPLE_MOVING_AVERAGE', 'WEIGHTED_MOVING_AVERAGE')
        AND periodos >= 1 AND alfa IS NULL AND beta IS NULL AND longitud_ciclo IS NULL)
    OR (metodo = 'EXPONENTIAL_SMOOTHING'
        AND periodos IS NULL AND alfa > 0 AND alfa < 1 AND beta IS NULL AND longitud_ciclo IS NULL)
    OR (metodo = 'TREND_ADJUSTED_EXPONENTIAL_SMOOTHING'
        AND periodos IS NULL AND alfa > 0 AND alfa < 1 AND beta > 0 AND beta < 1 AND longitud_ciclo IS NULL)
    OR (metodo = 'LINEAR_REGRESSION'
        AND periodos IS NULL AND alfa IS NULL AND beta IS NULL AND longitud_ciclo IS NULL)
    OR (metodo = 'SEASONAL_MULTIPLICATIVE'
        AND periodos IS NULL AND alfa IS NULL AND beta IS NULL AND longitud_ciclo >= 2));
