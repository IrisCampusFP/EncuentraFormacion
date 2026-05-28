-- V8: Tabla maestra m_tipo_estudios — separa el TIPO de enseñanza que ofrece
-- una formación del GRADO requerido para matricularse en ella.
-- Hasta ahora ambos conceptos compartían m_grado_estudios.

SET search_path TO encuentra_formacion;

-- ─────────────────────────────────────────────────────────────────
-- 1. Nueva tabla maestra: m_tipo_estudios
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE m_tipo_estudios (
    id_tipo_estudios BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre           VARCHAR(80) NOT NULL UNIQUE
);

-- ─────────────────────────────────────────────────────────────────
-- 2. Datos de referencia
-- ─────────────────────────────────────────────────────────────────
INSERT INTO m_tipo_estudios (id_tipo_estudios, nombre) OVERRIDING SYSTEM VALUE VALUES
    -- Enseñanzas no universitarias (LOMLOE)
    (1,  'Educación Infantil 1er ciclo'),
    (2,  'Educación Infantil 2º ciclo'),
    (3,  'Educación Primaria'),
    (4,  'ESO'),
    (5,  'Bachillerato'),
    (6,  'FP Básica'),
    (7,  'FP Grado Medio'),
    (8,  'FP Grado Superior'),
    (9,  'Educación Especial'),
    (10, 'Idiomas'),
    (11, 'Música y Artes'),
    (12, 'Enseñanzas Deportivas'),
    (13, 'Educación de Adultos'),
    -- Formación continua y no reglada
    (14, 'Curso / Formación no reglada'),
    (15, 'Certificado de Profesionalidad'),
    -- Enseñanzas universitarias (LOU)
    (16, 'Grado Universitario'),
    (17, 'Máster Oficial'),
    (18, 'Máster / Título propio'),
    (19, 'Doctorado');

-- ─────────────────────────────────────────────────────────────────
-- 3. Añadir FP Básica a m_grado_estudios (nivel de estudios previo)
-- ─────────────────────────────────────────────────────────────────
INSERT INTO m_grado_estudios (id_grado_estudios, nombre) OVERRIDING SYSTEM VALUE VALUES
    (11, 'FP Básica');

-- ─────────────────────────────────────────────────────────────────
-- 4. Migrar formaciones.tipo_estudios de m_grado_estudios → m_tipo_estudios
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE formaciones DROP CONSTRAINT fk_formaciones_tipo_estudios;

-- Mapeo: IDs antiguos (m_grado_estudios) → IDs nuevos (m_tipo_estudios)
--   2 Primaria       → 3  Educación Primaria
--   3 ESO            → 4  ESO
--   4 Bachillerato   → 5  Bachillerato
--   5 Grado Medio    → 7  FP Grado Medio
--   6 Grado Superior → 8  FP Grado Superior
--   7 Universidad    → 16 Grado Universitario
--   8 Máster         → 17 Máster Oficial
--   9 Curso          → 14 Curso / Formación no reglada
UPDATE formaciones
SET tipo_estudios = CASE tipo_estudios
    WHEN 2  THEN 3
    WHEN 3  THEN 4
    WHEN 4  THEN 5
    WHEN 5  THEN 7
    WHEN 6  THEN 8
    WHEN 7  THEN 16
    WHEN 8  THEN 17
    WHEN 9  THEN 14
    ELSE NULL
END
WHERE tipo_estudios IS NOT NULL;

ALTER TABLE formaciones
    ADD CONSTRAINT fk_formaciones_tipo_estudios
    FOREIGN KEY (tipo_estudios)
    REFERENCES m_tipo_estudios(id_tipo_estudios) ON DELETE SET NULL;

-- ─────────────────────────────────────────────────────────────────
-- 5. Añadir MIXTO al CHECK de centros.tipo
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE centros DROP CONSTRAINT centros_tipo_check;
ALTER TABLE centros
    ADD CONSTRAINT centros_tipo_check
    CHECK (tipo IN ('PUBLICO', 'PRIVADO', 'CONCERTADO', 'MIXTO'));
