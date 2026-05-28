-- Impide que un usuario tenga más de una solicitud PENDIENTE para el mismo centro.
-- Un índice parcial es la forma correcta en PostgreSQL: no bloquea re-solicitar tras
-- rechazo o cancelación, solo evita duplicados activos.
CREATE UNIQUE INDEX uq_solicitud_pendiente_usuario_centro
    ON solicitudes_gestion (usuario_id, centro_id)
    WHERE estado = 'PENDIENTE';
