-- V2: Nuevas tablas para chat, FAQ y notificaciones (Tracks 2 y 3)

SET search_path TO encuentra_formacion;

-- ─────────────────────────────────────────────────────────────────
-- Chat: conversaciones estudiante ↔ centro
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE conversaciones (
    id_conversacion  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estudiante_id    BIGINT    NOT NULL,
    centro_id        BIGINT    NOT NULL,
    fecha_inicio     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_actividad TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (estudiante_id, centro_id),
    CONSTRAINT fk_conv_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_conv_centro FOREIGN KEY (centro_id)
        REFERENCES centros(id_centro) ON DELETE CASCADE
);

CREATE TABLE mensajes (
    id_mensaje      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversacion_id BIGINT    NOT NULL,
    remitente_id    BIGINT    NOT NULL,
    contenido       TEXT      NOT NULL,
    fecha_envio     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leido           BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_mensaje_conversacion FOREIGN KEY (conversacion_id)
        REFERENCES conversaciones(id_conversacion) ON DELETE CASCADE,
    CONSTRAINT fk_mensaje_remitente FOREIGN KEY (remitente_id)
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

CREATE INDEX idx_mensajes_conversacion     ON mensajes(conversacion_id);
CREATE INDEX idx_mensajes_no_leidos        ON mensajes(conversacion_id, leido) WHERE leido = false;
CREATE INDEX idx_conversaciones_estudiante ON conversaciones(estudiante_id);
CREATE INDEX idx_conversaciones_centro     ON conversaciones(centro_id);

-- ─────────────────────────────────────────────────────────────────
-- FAQ pública del centro
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE faq_centro (
    id_faq     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    centro_id  BIGINT       NOT NULL,
    pregunta   VARCHAR(500) NOT NULL,
    respuesta  TEXT         NOT NULL,
    orden      INT          NOT NULL DEFAULT 0,
    activa     BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_alta TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_faq_centro FOREIGN KEY (centro_id)
        REFERENCES centros(id_centro) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────────
-- Notificaciones de usuario
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE notificaciones (
    id_notificacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL,
    tipo            VARCHAR(50)  NOT NULL
                        CHECK (tipo IN ('SOLICITUD_APROBADA', 'SOLICITUD_RECHAZADA', 'NUEVO_MENSAJE')),
    titulo          VARCHAR(255) NOT NULL,
    mensaje         TEXT         NOT NULL,
    url_referencia  VARCHAR(500),
    leida           BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

CREATE INDEX idx_notificaciones_usuario   ON notificaciones(usuario_id, fecha_creacion DESC);
CREATE INDEX idx_notificaciones_no_leidas ON notificaciones(usuario_id) WHERE leida = false;
