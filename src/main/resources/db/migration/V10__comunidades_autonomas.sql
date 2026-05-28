-- V10: Tabla maestra m_comunidades_autonomas + FK en m_provincias
-- Añade la comunidad autónoma como entidad de primer nivel para filtrar
-- centros y formaciones por CCAA en el buscador.

SET search_path TO encuentra_formacion;

-- ─────────────────────────────────────────────────────────────────
-- 1. Tabla maestra
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE m_comunidades_autonomas (
    id_comunidad_autonoma BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre                VARCHAR(100) NOT NULL UNIQUE,
    codigo_ine            CHAR(2)      NOT NULL UNIQUE
);

-- ─────────────────────────────────────────────────────────────────
-- 2. Datos de referencia (INE — Real Decreto 1997/1978 y ss.)
-- ─────────────────────────────────────────────────────────────────
INSERT INTO m_comunidades_autonomas (nombre, codigo_ine) VALUES
    ('Andalucía',                      '01'),
    ('Aragón',                         '02'),
    ('Asturias, Principado de',        '03'),
    ('Balears, Illes',                 '04'),
    ('Canarias',                       '05'),
    ('Cantabria',                      '06'),
    ('Castilla y León',                '07'),
    ('Castilla-La Mancha',             '08'),
    ('Cataluña',                       '09'),
    ('Comunitat Valenciana',           '10'),
    ('Extremadura',                    '11'),
    ('Galicia',                        '12'),
    ('Madrid, Comunidad de',           '13'),
    ('Murcia, Región de',              '14'),
    ('Navarra, Comunidad Foral de',    '15'),
    ('País Vasco',                     '16'),
    ('Rioja, La',                      '17'),
    ('Ceuta',                          '18'),
    ('Melilla',                        '19');

-- ─────────────────────────────────────────────────────────────────
-- 3. Añadir FK a m_provincias
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE m_provincias
    ADD COLUMN comunidad_autonoma_id BIGINT;

-- ─────────────────────────────────────────────────────────────────
-- 4. Asignar comunidad a cada provincia (código INE oficial)
-- ─────────────────────────────────────────────────────────────────
UPDATE m_provincias p
SET comunidad_autonoma_id = ca.id_comunidad_autonoma
FROM m_comunidades_autonomas ca
WHERE ca.codigo_ine = CASE p.codigo_ine
    -- Andalucía
    WHEN '04' THEN '01'  -- Almería
    WHEN '11' THEN '01'  -- Cádiz
    WHEN '14' THEN '01'  -- Córdoba
    WHEN '18' THEN '01'  -- Granada
    WHEN '21' THEN '01'  -- Huelva
    WHEN '23' THEN '01'  -- Jaén
    WHEN '29' THEN '01'  -- Málaga
    WHEN '41' THEN '01'  -- Sevilla
    -- Aragón
    WHEN '22' THEN '02'  -- Huesca
    WHEN '44' THEN '02'  -- Teruel
    WHEN '50' THEN '02'  -- Zaragoza
    -- Asturias
    WHEN '33' THEN '03'
    -- Balears
    WHEN '07' THEN '04'
    -- Canarias
    WHEN '35' THEN '05'  -- Las Palmas
    WHEN '38' THEN '05'  -- Santa Cruz de Tenerife
    -- Cantabria
    WHEN '39' THEN '06'
    -- Castilla y León
    WHEN '05' THEN '07'  -- Ávila
    WHEN '09' THEN '07'  -- Burgos
    WHEN '24' THEN '07'  -- León
    WHEN '34' THEN '07'  -- Palencia
    WHEN '37' THEN '07'  -- Salamanca
    WHEN '40' THEN '07'  -- Segovia
    WHEN '42' THEN '07'  -- Soria
    WHEN '47' THEN '07'  -- Valladolid
    WHEN '49' THEN '07'  -- Zamora
    -- Castilla-La Mancha
    WHEN '02' THEN '08'  -- Albacete
    WHEN '13' THEN '08'  -- Ciudad Real
    WHEN '16' THEN '08'  -- Cuenca
    WHEN '19' THEN '08'  -- Guadalajara
    WHEN '45' THEN '08'  -- Toledo
    -- Cataluña
    WHEN '08' THEN '09'  -- Barcelona
    WHEN '17' THEN '09'  -- Girona
    WHEN '25' THEN '09'  -- Lleida
    WHEN '43' THEN '09'  -- Tarragona
    -- Comunitat Valenciana
    WHEN '03' THEN '10'  -- Alicante
    WHEN '12' THEN '10'  -- Castellón
    WHEN '46' THEN '10'  -- Valencia
    -- Extremadura
    WHEN '06' THEN '11'  -- Badajoz
    WHEN '10' THEN '11'  -- Cáceres
    -- Galicia
    WHEN '15' THEN '12'  -- A Coruña
    WHEN '27' THEN '12'  -- Lugo
    WHEN '32' THEN '12'  -- Ourense
    WHEN '36' THEN '12'  -- Pontevedra
    -- Madrid
    WHEN '28' THEN '13'
    -- Murcia
    WHEN '30' THEN '14'
    -- Navarra
    WHEN '31' THEN '15'
    -- País Vasco
    WHEN '01' THEN '16'  -- Álava
    WHEN '20' THEN '16'  -- Gipuzkoa
    WHEN '48' THEN '16'  -- Bizkaia
    -- La Rioja
    WHEN '26' THEN '17'
    -- Ceuta
    WHEN '51' THEN '18'
    -- Melilla
    WHEN '52' THEN '19'
END;

-- ─────────────────────────────────────────────────────────────────
-- 5. Activar NOT NULL y FK constraint
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE m_provincias
    ALTER COLUMN comunidad_autonoma_id SET NOT NULL;

ALTER TABLE m_provincias
    ADD CONSTRAINT fk_provincias_comunidad_autonoma
    FOREIGN KEY (comunidad_autonoma_id)
    REFERENCES m_comunidades_autonomas(id_comunidad_autonoma)
    ON DELETE RESTRICT;
