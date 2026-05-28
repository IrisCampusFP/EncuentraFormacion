-- V12: Hace nullable la columna horario en formaciones.
-- Los datos scrapeados de fuentes oficiales no incluyen el horario real,
-- que solo puede conocerse consultando directamente al centro.
-- Un gestor podrá completarlo al gestionar su centro.

ALTER TABLE encuentra_formacion.formaciones
    ALTER COLUMN horario DROP NOT NULL;

ALTER TABLE encuentra_formacion.formaciones
    DROP CONSTRAINT IF EXISTS formaciones_horario_check;

ALTER TABLE encuentra_formacion.formaciones
    ADD CONSTRAINT formaciones_horario_check
    CHECK (horario IS NULL OR horario IN ('MANANA', 'TARDE', 'NOCHE', 'FLEXIBLE'));
