-- V9: Moderniza m_grado_estudios para reflejar la nomenclatura vigente del sistema educativo español.

SET search_path TO encuentra_formacion;

-- Actualizar nombres a nomenclatura post-Bolonia y LOMLOE
UPDATE m_grado_estudios SET nombre = 'Grado Universitario' WHERE id_grado_estudios = 7;
UPDATE m_grado_estudios SET nombre = 'Máster Oficial'      WHERE id_grado_estudios = 8;

-- Eliminar 'Curso' (id=9): no es un nivel académico acreditado.
-- Los estudiantes con este valor quedan sin nivel especificado.
UPDATE estudiantes SET grado_estudios_id = NULL WHERE grado_estudios_id = 9;
DELETE FROM m_grado_estudios WHERE id_grado_estudios = 9;
