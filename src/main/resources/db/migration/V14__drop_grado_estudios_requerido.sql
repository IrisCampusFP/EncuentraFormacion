-- V14: Elimina grado_estudios_requerido_id de formaciones.
-- El requisito de acceso es siempre derivable del tipo_estudios (redundante).
-- m_grado_estudios se mantiene: la usan los estudiantes para su propio nivel académico.

SET search_path TO encuentra_formacion;

ALTER TABLE formaciones DROP COLUMN IF EXISTS grado_estudios_requerido_id;
