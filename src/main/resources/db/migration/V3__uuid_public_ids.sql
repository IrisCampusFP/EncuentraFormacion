SET search_path TO encuentra_formacion;

ALTER TABLE formaciones
    ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX idx_formaciones_uuid ON formaciones(uuid);

ALTER TABLE centros
    ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX idx_centros_uuid ON centros(uuid);
