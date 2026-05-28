CREATE TABLE IF NOT EXISTS eventos_solicitud_chat (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversacion_id BIGINT      NOT NULL,
    solicitud_id    BIGINT      NOT NULL,
    tipo_evento     VARCHAR(30) NOT NULL,
    formacion_nombre VARCHAR(255) NOT NULL,
    fecha           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evento_conversacion
        FOREIGN KEY (conversacion_id) REFERENCES conversaciones(id_conversacion) ON DELETE CASCADE,
    CONSTRAINT fk_evento_solicitud
        FOREIGN KEY (solicitud_id)    REFERENCES solicitudes_formacion(id_solicitud) ON DELETE CASCADE,
    CONSTRAINT chk_tipo_evento
        CHECK (tipo_evento IN ('SOLICITUD_ENVIADA','SOLICITUD_CANCELADA','SOLICITUD_ACEPTADA','SOLICITUD_RECHAZADA'))
);

CREATE INDEX idx_evento_conversacion ON eventos_solicitud_chat(conversacion_id);
