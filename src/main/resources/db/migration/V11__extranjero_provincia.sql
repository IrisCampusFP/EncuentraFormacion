-- V11: Añade CCAA y provincia "Extranjero" para centros educativos españoles en el exterior
-- (colegios en el extranjero, institutos Cervantes, IESE campus internacionales, etc.)
-- Código INE ficticio '00' para CCAA y '60' para provincia (coincide con el prefijo SIRECEC real)

INSERT INTO m_comunidades_autonomas (nombre, codigo_ine)
VALUES ('Extranjero', '00');

INSERT INTO m_provincias (nombre, codigo_ine, comunidad_autonoma_id)
VALUES (
    'Extranjero',
    '60',
    (SELECT id_comunidad_autonoma FROM m_comunidades_autonomas WHERE codigo_ine = '00')
);
