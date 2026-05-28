SET search_path TO encuentra_formacion;

CREATE TABLE sesiones_ia (
    id_sesion        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estudiante_id    BIGINT       NOT NULL,
    titulo           VARCHAR(255) NOT NULL DEFAULT 'Nueva conversación',
    fecha_inicio     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_actividad TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sesion_ia_estudiante
        FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE
);

CREATE INDEX idx_sesiones_ia_estudiante_actividad
    ON sesiones_ia(estudiante_id, ultima_actividad DESC);

CREATE TABLE mensajes_ia (
    id_mensaje  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sesion_id   BIGINT   NOT NULL,
    rol         VARCHAR(10) NOT NULL CHECK (rol IN ('USER', 'ASSISTANT')),
    contenido   TEXT     NOT NULL,
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mensaje_ia_sesion
        FOREIGN KEY (sesion_id) REFERENCES sesiones_ia(id_sesion) ON DELETE CASCADE
);

CREATE INDEX idx_mensajes_ia_sesion_fecha
    ON mensajes_ia(sesion_id, fecha_envio ASC);
