-- Añade ubicación al perfil del estudiante

ALTER TABLE estudiantes
    ADD COLUMN provincia_id BIGINT,
    ADD COLUMN localidad    VARCHAR(100);

ALTER TABLE estudiantes
    ADD CONSTRAINT fk_estudiantes_provincia
        FOREIGN KEY (provincia_id)
        REFERENCES m_provincias(id_provincia)
        ON DELETE SET NULL;
