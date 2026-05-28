SET search_path TO encuentra_formacion;

CREATE TABLE conversacion_formaciones (
    conversacion_id  BIGINT NOT NULL,
    formacion_id     BIGINT NOT NULL,
    fecha_vinculo    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversacion_id, formacion_id),
    CONSTRAINT fk_cf_conversacion FOREIGN KEY (conversacion_id)
        REFERENCES conversaciones(id_conversacion) ON DELETE CASCADE,
    CONSTRAINT fk_cf_formacion FOREIGN KEY (formacion_id)
        REFERENCES formaciones(id_formacion) ON DELETE CASCADE
);

CREATE INDEX idx_cf_conversacion ON conversacion_formaciones(conversacion_id);
CREATE INDEX idx_cf_formacion     ON conversacion_formaciones(formacion_id);
