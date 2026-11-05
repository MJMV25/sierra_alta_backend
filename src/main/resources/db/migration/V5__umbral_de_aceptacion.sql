-- Umbral de aceptación del MAPE (fase 3, ADR 07 pregunta 21).
-- Cada ejecución guarda el umbral vigente al ejecutarse, como fracción igual que el MAPE (0,10 = 10 %),
-- para que cambiar la configuración no altere el estado de las ejecuciones anteriores. Nulo en las
-- ejecuciones hechas antes de fijarlo: el sistema las muestra "sin criterio definido".
ALTER TABLE ejecucion ADD COLUMN umbral_mape NUMERIC(11, 10);
ALTER TABLE ejecucion ADD CONSTRAINT ck_ejecucion_umbral_mape
    CHECK (umbral_mape IS NULL OR (umbral_mape > 0 AND umbral_mape < 1));
