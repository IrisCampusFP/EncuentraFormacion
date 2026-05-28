-- SEC-07: Tabla de tokens JWT invalidados (OWASP A02/A07)
-- Almacena el hash SHA-256 de los tokens invalidados en logout.
-- La comprobación en JwtAuthenticationFilter protege contra tokens robados
-- que aún no han expirado tras el cierre de sesión.

CREATE TABLE encuentra_formacion.tokens_invalidados (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash          VARCHAR(64)  NOT NULL UNIQUE,  -- SHA-256 en Base64 (nunca el token en claro)
    fecha_invalidacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion    TIMESTAMP    NOT NULL           -- Expiración original del JWT para limpieza
);

-- Índice para la comprobación rápida en cada petición autenticada
CREATE INDEX idx_tokens_invalidados_hash
    ON encuentra_formacion.tokens_invalidados (token_hash);

-- Índice para la limpieza nocturna de tokens ya expirados
CREATE INDEX idx_tokens_invalidados_expiracion
    ON encuentra_formacion.tokens_invalidados (fecha_expiracion);
