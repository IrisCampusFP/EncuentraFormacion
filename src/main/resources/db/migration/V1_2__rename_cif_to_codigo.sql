-- V1_2: Rename centros.cif → centros.codigo + refinar tipo
-- El campo almacena el código oficial del Ministerio de Educación (SIRECEC, 8 dígitos numéricos).
-- Todos los centros autorizados en España tienen este código: formato PP + 6 dígitos secuenciales.

SET search_path TO encuentra_formacion;

ALTER TABLE centros RENAME COLUMN cif TO codigo;

ALTER TABLE centros ALTER COLUMN codigo TYPE CHAR(8);

ALTER TABLE centros ADD CONSTRAINT chk_centros_codigo_formato
    CHECK (codigo ~ '^\d{8}$');
