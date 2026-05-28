-- DATOS DE EJEMPLO — EncuentraFormación
-- Ejecución manual para entorno de desarrollo (NO gestionado por Flyway)
-- Contraseña de todos los usuarios de prueba: 1234
-- Hash bcrypt: $2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe

SET search_path TO encuentra_formacion;

-- Limpia datos de ejemplo previos para permitir re-ejecución idempotente
TRUNCATE TABLE
    formaciones_favoritas, valoraciones, solicitudes_formacion,
    mensajes, conversaciones, notificaciones,
    formaciones, gestores_centros, solicitudes_gestion,
    faq_centro, centros,
    estudiantes, rol_usuario, usuarios
RESTART IDENTITY CASCADE;

-- ── Usuarios básicos de prueba ─────────────────────────────────
INSERT INTO usuarios (email, password_hash, nombre, apellidos, username, fecha_nacimiento, telefono, dni, sexo, activo)
VALUES
    ('ana.garcia@example.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Ana',    'García López',     'anagarcia',   '2001-05-14', '+34 612 345 678', '12345678Z', 'FEMENINO',  TRUE),
    ('david.martin@example.com',  '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'David',  'Martín Pérez',     'davidmartin', '1998-11-02', '+34 622 111 222', '87654321X', 'MASCULINO', TRUE),
    ('lucia.sanchez@example.com', '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Lucía',  'Sánchez Ruiz',     'luciasr',     '2003-02-20', '+34 633 222 333', '11223344A', 'FEMENINO',  TRUE),
    ('alba.ramos@example.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Alba',   'Ramos Gil',        'albaramos',   '2000-03-05', '+34 688 777 888', '66778899F', 'OTRO',      TRUE),
    ('paula.navarro@example.com', '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Paula',  'Navarro Torres',   'paulanav',    '1995-07-09', '+34 644 333 444', '22334455B', 'FEMENINO',  TRUE),
    ('carlos.romero@example.com', '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Carlos', 'Romero Díaz',      'cromero',     '1989-01-23', '+34 655 444 555', '33445566C', 'MASCULINO', TRUE),
    ('marta.ortega@example.com',  '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Marta',  'Ortega Molina',    'mortega',     '1992-09-30', '+34 666 555 666', '44556677D', 'FEMENINO',  TRUE),
    ('iris.perez@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Iris',   'Pérez Aparicio',   'iperez',      '2005-12-10', '+34 677 666 777', '55667788E', 'FEMENINO',  TRUE);

-- ── Asignación de roles ────────────────────────────────────────
INSERT INTO rol_usuario (usuario_id, rol_id) VALUES
    (8, 1),
    (1, 2), (2, 2), (3, 2), (4, 2),
    (8, 2),
    (5, 3), (6, 3), (7, 3);

-- ── Perfiles de estudiante ─────────────────────────────────────
INSERT INTO estudiantes (usuario_id, grado_estudios_id) VALUES
    (1, 4),
    (2, 6),
    (3, 3),
    (4, NULL),
    (8, 6);

-- ── Centros de prueba ──────────────────────────────────────────
INSERT INTO centros (nombre_comercial, codigo, descripcion, direccion, localidad, provincia_id, tipo, telefono, email, pagina_web, verificado, fecha_verificacion, tiene_gestor)
VALUES
    ('Institut Tecnològic de Barcelona', '08000001',
     'Centro de formación tecnológica con ciclos de FP e itinerarios de especialización.',
     'Carrer d''Aragó 55', 'Barcelona', (SELECT id_provincia FROM m_provincias WHERE nombre = 'Barcelona'), 'PUBLICO',
     '+34 931 000 001', 'contacto.itb@example.com', 'https://example.com/itb',
     TRUE, '2025-06-15 10:00:00', TRUE),
    ('Centro de Formación Madrid Norte', '28000001',
     'Centro privado orientado a certificaciones y cursos intensivos.',
     'Calle de Bravo Murillo 120', 'Madrid', (SELECT id_provincia FROM m_provincias WHERE nombre = 'Madrid'), 'PRIVADO',
     '+34 910 000 002', 'info.madnorte@example.com', 'https://example.com/madnorte',
     TRUE, '2025-08-20 14:30:00', TRUE),
    ('Escuela Profesional Valencia', '46000001',
     'Centro concertado con oferta de ciclos formativos.',
     'Avinguda del Cid 10', 'València', (SELECT id_provincia FROM m_provincias WHERE nombre = 'Valencia'), 'CONCERTADO',
     '+34 960 000 003', 'secretaria.epv@example.com', 'https://example.com/epv',
     TRUE, '2025-07-10 09:00:00', TRUE);

-- ── Solicitud de gestión de prueba ─────────────────────────────
INSERT INTO solicitudes_gestion (usuario_id, centro_id, prueba_titularidad, estado, fecha_solicitud)
VALUES (1, 1,
    decode('iVBORw0KGgoAAAANSUhEUgAAAHgAAACgCAMAAADw11iiAAAAD1BMVEXw8PD////IyMhkZGQyMsj4LwLTAAAArklEQVR4nO3cMQ6DMAAEQZz4/29OFREcQEICzFmzHVCM3BxUTJMkBfe6oXW4XB4YDAaDwWDw8+D3svZ65WbZvfn8E4PB4HzYcoHB4DzYcoHB4DzYcoHB4DzYcoHB4DzYcoHB4DzYcoHB4DzYcoHB4DzYcoHB4Dw4eLnqt2MnPs9t5Ivhumx8uLbdBP+5PzIYDAYflOdHg251P7jb+7j0+wLZCgwGDwv3+mWDJIX0AVwiQeh1ILYZAAAAAElFTkSuQmCC', 'base64'),
    'PENDIENTE', NOW());

-- ── Gestores asignados ─────────────────────────────────────────
INSERT INTO gestores_centros (usuario_id, centro_id) VALUES
    (5, 1), (6, 2), (7, 3);

-- ── Formaciones de prueba ──────────────────────────────────────
INSERT INTO formaciones (centro_id, grado_estudios_requerido_id, nombre, titulo_oficial, descripcion, tipo_estudios, horario, duracion_horas, activa, precio, modalidad, fecha_inicio, fecha_fin)
VALUES
    (1, 4, 'Desarrollo de Aplicaciones Web (DAW)',
     'Técnico Superior en Desarrollo de Aplicaciones Web',
     'Ciclo orientado a desarrollo web, bases de datos y despliegue.', 8, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
    (1, 3, 'Sistemas Microinformáticos y Redes (SMR)',
     'Técnico en Sistemas Microinformáticos y Redes',
     'Instalación, configuración y mantenimiento de sistemas y redes.', 7, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
    (2, NULL, 'Curso intensivo de Python para análisis de datos', NULL,
     'Python, pandas y fundamentos de análisis.', 14, 'FLEXIBLE', 120, TRUE, 490.00, 'SEMIPRESENCIAL', '2026-03-10', '2026-05-10'),
    (3, 5, 'Máster de especialización en Ciberseguridad (título propio)',
     'Máster (título propio)', 'Especialización con laboratorio y respuesta a incidentes.',
     15, 'NOCHE', 600, TRUE, 3200.00, 'DISTANCIA', '2026-02-15', '2026-11-30'),
    (2, 4, 'Administración de Sistemas Informáticos en Red (ASIR)',
     'Técnico Superior en Administración de Sistemas Informáticos en Red',
     'Sistemas, redes, virtualización y seguridad.', 8, 'MANANA', 2000, FALSE, 0.00, 'PRESENCIAL', '2024-09-15', '2026-06-15');

-- ── Solicitudes de admisión de prueba ──────────────────────────
INSERT INTO solicitudes_formacion (estudiante_id, formacion_id, estado, fecha_solicitud, fecha_respuesta)
VALUES
    (1, 1, 'PENDIENTE',  '2026-02-20 10:15:00', NULL),
    (2, 2, 'ACEPTADA',   '2026-01-31 09:00:00', '2026-02-10 12:30:00'),
    (3, 3, 'RECHAZADA',  '2026-01-15 16:20:00', '2026-01-20 11:05:00'),
    (4, 4, 'CANCELADA',  '2026-02-05 18:45:00', '2026-02-06 08:10:00');

-- ── Valoraciones de prueba ─────────────────────────────────────
INSERT INTO valoraciones (estudiante_id, formacion_id, estrellas, comentario, fecha)
VALUES
    (2, 2, 5, 'Contenido actualizado y buen enfoque práctico.', '2026-02-15 14:00:00'),
    (3, 3, 2, 'Ritmo demasiado rápido y faltó acompañamiento.',  '2026-01-29 19:30:00');

-- ── Formaciones favoritas de prueba ────────────────────────────
INSERT INTO formaciones_favoritas (estudiante_id, formacion_id) VALUES
    (1, 1), (1, 3), (4, 4);


-- ============================================================
-- DATOS DE EJEMPLO MASIVOS - EncuentraFormación
-- PostgreSQL · Schema: encuentra_formacion
-- Contraseña de todos los usuarios: 1234
-- Hash bcrypt: $2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe
-- ============================================================

SET search_path TO encuentra_formacion;

-- ============================================================
-- USUARIOS (80 usuarios: 1 admin ya existente, 50 estudiantes, 29 gestores)
-- ============================================================
INSERT INTO usuarios (email, password_hash, nombre, apellidos, username, fecha_nacimiento, telefono, dni, sexo, activo, fecha_alta, ultima_conexion) VALUES

-- ESTUDIANTES (ids 9-58)
('sofia.blanco@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Sofía',     'Blanco Herrera',     'sofiablanco',    '2003-04-12', '+34 611 001 001', '10000009A', 'FEMENINO',  TRUE,  '2025-09-01 08:10:00', '2026-04-20 10:30:00'),
('marcos.fuentes@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Marcos',    'Fuentes Castillo',   'marcosfuentes',  '2001-11-25', '+34 611 001 002', '10000010B', 'MASCULINO', TRUE,  '2025-09-02 09:15:00', '2026-04-19 11:00:00'),
('elena.vega@gmail.com',         '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Elena',     'Vega Moreno',        'elenavega',      '2002-06-08', '+34 611 001 003', '10000011C', 'FEMENINO',  TRUE,  '2025-09-03 10:00:00', '2026-04-18 09:45:00'),
('pablo.iglesias@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Pablo',     'Iglesias Soto',      'pabloiglesias',  '2000-02-14', '+34 611 001 004', '10000012D', 'MASCULINO', TRUE,  '2025-09-04 11:20:00', '2026-04-17 14:20:00'),
('claudia.reyes@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Claudia',   'Reyes Montero',      'claudiareyes',   '2004-08-19', '+34 611 001 005', '10000013E', 'FEMENINO',  TRUE,  '2025-09-05 08:30:00', '2026-04-16 16:00:00'),
('ivan.cano@gmail.com',          '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Iván',      'Cano Delgado',       'ivancano',       '1999-12-03', '+34 611 001 006', '10000014F', 'MASCULINO', TRUE,  '2025-09-06 12:00:00', '2026-04-15 10:10:00'),
('nerea.campos@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Nerea',     'Campos Lara',        'nereacampos',    '2002-03-27', '+34 611 001 007', '10000015G', 'FEMENINO',  TRUE,  '2025-09-07 09:45:00', '2026-04-14 09:00:00'),
('sergio.mendez@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Sergio',    'Méndez Prieto',      'sergiomendez',   '2001-07-15', '+34 611 001 008', '10000016H', 'MASCULINO', TRUE,  '2025-09-08 10:30:00', '2026-04-13 17:30:00'),
('raquel.gimenez@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Raquel',    'Giménez Rubio',      'raquelgimenez',  '2003-09-22', '+34 611 001 009', '10000017I', 'FEMENINO',  TRUE,  '2025-09-09 11:15:00', '2026-04-12 12:00:00'),
('hugo.parra@gmail.com',         '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Hugo',      'Parra Expósito',     'hugoparra',      '2000-05-01', '+34 611 001 010', '10000018J', 'MASCULINO', TRUE,  '2025-09-10 08:00:00', '2026-04-11 08:30:00'),
('irene.pascual@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Irene',     'Pascual Medina',     'irenepascual',   '2004-01-30', '+34 611 001 011', '10000019K', 'FEMENINO',  TRUE,  '2025-09-11 09:00:00', '2026-04-10 15:45:00'),
('alejandro.vidal@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Alejandro', 'Vidal Guerrero',     'alejandrovidal', '2002-10-11', '+34 611 001 012', '10000020L', 'MASCULINO', TRUE,  '2025-09-12 10:45:00', '2026-04-09 11:20:00'),
('patricia.morin@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Patricia',  'Morín Serrano',      'patriciamorin',  '2001-04-05', '+34 611 001 013', '10000021M', 'FEMENINO',  FALSE, '2025-09-13 11:30:00', '2026-03-01 09:00:00'),
('diego.carrasco@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Diego',     'Carrasco Nieto',     'diegocarrasco',  '2003-07-18', '+34 611 001 014', '10000022N', 'MASCULINO', TRUE,  '2025-09-14 08:20:00', '2026-04-08 10:00:00'),
('veronica.santos@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Verónica',  'Santos Peñas',       'veronicasantos', '2000-11-09', '+34 611 001 015', '10000023O', 'FEMENINO',  TRUE,  '2025-09-15 09:10:00', '2026-04-07 16:15:00'),
('antonio.flores@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Antonio',   'Flores Vargas',      'antonioflores',  '1998-03-23', '+34 611 001 016', '10000024P', 'MASCULINO', TRUE,  '2025-09-16 10:00:00', '2026-04-06 09:30:00'),
('beatriz.leon@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Beatriz',   'León Cabrera',       'beatrizleon',    '2003-06-14', '+34 611 001 017', '10000025Q', 'FEMENINO',  TRUE,  '2025-09-17 11:00:00', '2026-04-05 14:00:00'),
('javier.muñoz@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Javier',    'Muñoz Esteve',       'javiermunoz',    '2001-08-28', '+34 611 001 018', '10000026R', 'MASCULINO', TRUE,  '2025-09-18 08:45:00', '2026-04-04 11:00:00'),
('amparo.gil@gmail.com',         '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Amparo',    'Gil Ferrer',         'amparogil',      '2002-12-02', '+34 611 001 019', '10000027S', 'FEMENINO',  TRUE,  '2025-09-19 09:30:00', '2026-04-03 10:30:00'),
('ruben.sanz@gmail.com',         '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Rubén',     'Sanz Aguilar',       'rubensanz',      '2000-09-16', '+34 611 001 020', '10000028T', 'MASCULINO', FALSE, '2025-09-20 10:15:00', '2026-02-15 08:00:00'),
('miriam.ibañez@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Miriam',    'Ibáñez Cortés',      'miriamibanez',   '2004-02-07', '+34 611 001 021', '10000029U', 'FEMENINO',  TRUE,  '2025-10-01 08:00:00', '2026-04-20 09:00:00'),
('gonzalo.peña@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Gonzalo',   'Peña Jiménez',       'gonzalopeña',    '2001-05-20', '+34 611 001 022', '10000030V', 'MASCULINO', TRUE,  '2025-10-02 09:00:00', '2026-04-19 10:00:00'),
('laura.molina@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Laura',     'Molina Roca',        'lauramolina',    '2003-09-03', '+34 611 001 023', '10000031W', 'FEMENINO',  TRUE,  '2025-10-03 10:00:00', '2026-04-18 11:00:00'),
('oscar.herrera@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Óscar',     'Herrera Abad',       'oscarherrera',   '1999-07-11', '+34 611 001 024', '10000032X', 'MASCULINO', TRUE,  '2025-10-04 11:00:00', '2026-04-17 12:00:00'),
('carmen.bravo@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Carmen',    'Bravo Alonso',       'carmenbravo',    '2002-04-25', '+34 611 001 025', '10000033Y', 'FEMENINO',  TRUE,  '2025-10-05 08:30:00', '2026-04-16 13:00:00'),
('nicolas.guerra@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Nicolás',   'Guerra Pinto',       'nicolasg',       '2000-01-17', '+34 611 001 026', '10000034Z', 'MASCULINO', TRUE,  '2025-10-06 09:30:00', '2026-04-15 14:30:00'),
('adriana.moya@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Adriana',   'Moya Fernández',     'adrianamoya',    '2003-11-08', '+34 611 001 027', '10000035A', 'FEMENINO',  TRUE,  '2025-10-07 10:30:00', '2026-04-14 15:00:00'),
('daniel.varela@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Daniel',    'Varela Cruz',        'danielvarela',   '2001-03-29', '+34 611 001 028', '10000036B', 'MASCULINO', TRUE,  '2025-10-08 11:30:00', '2026-04-13 16:00:00'),
('teresa.hurtado@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Teresa',    'Hurtado Piñero',     'teresahurtado',  '2004-06-16', '+34 611 001 029', '10000037C', 'FEMENINO',  TRUE,  '2025-10-09 08:00:00', '2026-04-12 17:00:00'),
('jorge.bolivar@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Jorge',     'Bolívar Calvo',      'jorgebolivar',   '2000-10-04', '+34 611 001 030', '10000038D', 'MASCULINO', TRUE,  '2025-10-10 09:00:00', '2026-04-11 08:00:00'),
('silvia.quintero@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Silvia',    'Quintero Baena',     'silviaquin',     '2002-08-21', '+34 611 001 031', '10000039E', 'FEMENINO',  TRUE,  '2025-10-11 10:00:00', '2026-04-10 09:00:00'),
('fernando.navas@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Fernando',  'Navas Daza',         'fernandona',     '1998-12-13', '+34 611 001 032', '10000040F', 'MASCULINO', TRUE,  '2025-10-12 11:00:00', '2026-04-09 10:00:00'),
('cristina.lozano@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Cristina',  'Lozano Miralles',    'cristinalozano', '2003-02-06', '+34 611 001 033', '10000041G', 'FEMENINO',  TRUE,  '2025-10-13 08:45:00', '2026-04-08 11:00:00'),
('miguel.palacios@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Miguel',    'Palacios Soler',     'miguelpalacios', '2001-06-24', '+34 611 001 034', '10000042H', 'MASCULINO', TRUE,  '2025-10-14 09:45:00', '2026-04-07 12:00:00'),
('emma.rodrigo@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Emma',      'Rodrigo Manzano',    'emmarodrigo',    '2004-09-10', '+34 611 001 035', '10000043I', 'FEMENINO',  FALSE, '2025-10-15 10:45:00', '2026-01-20 10:00:00'),
('victor.espinosa@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Víctor',    'Espinosa Toro',      'victorespinosa', '2000-04-02', '+34 611 001 036', '10000044J', 'MASCULINO', TRUE,  '2025-10-16 11:45:00', '2026-04-06 13:00:00'),
('alba.dominguez@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Alba',      'Domínguez Téllez',   'albadominguez',  '2002-11-18', '+34 611 001 037', '10000045K', 'FEMENINO',  TRUE,  '2025-10-17 08:00:00', '2026-04-05 14:00:00'),
('raul.iborra@gmail.com',        '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Raúl',      'Iborra Climent',     'rauliborra',     '2001-01-07', '+34 611 001 038', '10000046L', 'MASCULINO', TRUE,  '2025-10-18 09:00:00', '2026-04-04 15:00:00'),
('noelia.camacho@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Noelia',    'Camacho Furio',      'noeliacamacho',  '2003-05-29', '+34 611 001 039', '10000047M', 'FEMENINO',  TRUE,  '2025-10-19 10:00:00', '2026-04-03 16:00:00'),
('andres.llopis@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Andrés',    'Llopis Verdú',       'andresllopis',   '2000-08-14', '+34 611 001 040', '10000048N', 'MASCULINO', TRUE,  '2025-10-20 11:00:00', '2026-04-02 17:00:00'),
('lucia.trillo@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Lucía',     'Trillo Marín',       'luciatrillo',    '2004-03-22', '+34 611 001 041', '10000049O', 'FEMENINO',  TRUE,  '2025-10-21 08:30:00', '2026-04-01 09:00:00'),
('mario.alcala@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Mario',     'Alcalá Benito',      'marioalcala',    '2001-12-09', '+34 611 001 042', '10000050P', 'MASCULINO', TRUE,  '2025-10-22 09:30:00', '2026-03-31 10:00:00'),
('rocio.serrano@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Rocío',     'Serrano Palomares',  'rocioserrano',   '2002-07-05', '+34 611 001 043', '10000051Q', 'FEMENINO',  TRUE,  '2025-10-23 10:30:00', '2026-03-30 11:00:00'),
('carlos.mendoza@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Carlos',    'Mendoza Ag├╝era',     'carlosmendoza',  '2000-02-19', '+34 611 001 044', '10000052R', 'MASCULINO', TRUE,  '2025-10-24 11:30:00', '2026-03-29 12:00:00'),
('julia.estrada@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Julia',     'Estrada Solano',     'juliaestrada',   '2003-10-31', '+34 611 001 045', '10000053S', 'FEMENINO',  TRUE,  '2025-11-01 08:00:00', '2026-03-28 13:00:00'),
('pedro.zamora@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Pedro',     'Zamora Cervantes',   'pedrozamora',    '2001-09-17', '+34 611 001 046', '10000054T', 'MASCULINO', TRUE,  '2025-11-02 09:00:00', '2026-03-27 14:00:00'),
('ana.crespo@gmail.com',         '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Ana',       'Crespo Vallejo',     'anacrespo',      '2004-04-03', '+34 611 001 047', '10000055U', 'FEMENINO',  TRUE,  '2025-11-03 10:00:00', '2026-03-26 15:00:00'),
('roberto.gallego@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Roberto',   'Gallego Hierro',     'robertogallego', '2002-01-26', '+34 611 001 048', '10000056V', 'MASCULINO', TRUE,  '2025-11-04 11:00:00', '2026-03-25 16:00:00'),
('nuria.casado@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Nuria',     'Casado Pineda',      'nuriacasado',    '2000-06-12', '+34 611 001 049', '10000057W', 'FEMENINO',  TRUE,  '2025-11-05 08:30:00', '2026-03-24 17:00:00'),
('alberto.mora@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Alberto',   'Mora Escribano',     'albertomora',    '2003-08-08', '+34 611 001 050', '10000058X', 'MASCULINO', TRUE,  '2025-11-06 09:30:00', '2026-03-23 09:00:00'),

-- GESTORES (ids 59-87)
('ramon.solano@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Ramón',     'Solano Pedraza',     'ramonsolano',    '1980-03-11', '+34 622 001 001', '20000059A', 'MASCULINO', TRUE,  '2025-01-10 09:00:00', '2026-04-20 08:00:00'),
('ana.bustamante@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Ana',       'Bustamante Reig',    'anabustamante',  '1975-07-22', '+34 622 001 002', '20000060B', 'FEMENINO',  TRUE,  '2025-01-11 10:00:00', '2026-04-19 09:00:00'),
('jose.castellano@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'José',      'Castellano Vera',    'josecastellano', '1983-11-05', '+34 622 001 003', '20000061C', 'MASCULINO', TRUE,  '2025-01-12 11:00:00', '2026-04-18 10:00:00'),
('pilar.aguirre@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Pilar',     'Aguirre Esteban',    'pilaraguirre',   '1978-05-18', '+34 622 001 004', '20000062D', 'FEMENINO',  TRUE,  '2025-01-13 08:30:00', '2026-04-17 11:00:00'),
('luis.montes@gmail.com',        '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Luis',      'Montes Salas',       'luismontes',     '1985-09-30', '+34 622 001 005', '20000063E', 'MASCULINO', TRUE,  '2025-01-14 09:30:00', '2026-04-16 12:00:00'),
('esther.bernal@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Esther',    'Bernal Colom',       'estherbernal',   '1979-02-14', '+34 622 001 006', '20000064F', 'FEMENINO',  TRUE,  '2025-01-15 10:30:00', '2026-04-15 13:00:00'),
('ignacio.cabello@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Ignacio',   'Cabello Ros',        'ignaciocabello', '1982-06-25', '+34 622 001 007', '20000065G', 'MASCULINO', TRUE,  '2025-01-16 11:30:00', '2026-04-14 14:00:00'),
('monica.pons@gmail.com',        '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Mónica',    'Pons Alcover',       'monicapons',     '1977-10-08', '+34 622 001 008', '20000066H', 'FEMENINO',  TRUE,  '2025-01-17 08:00:00', '2026-04-13 15:00:00'),
('ernesto.trujillo@gmail.com',   '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Ernesto',   'Trujillo Arenas',    'ernestotrujillo','1986-04-19', '+34 622 001 009', '20000067I', 'MASCULINO', TRUE,  '2025-01-18 09:00:00', '2026-04-12 16:00:00'),
('consuelo.mir@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Consuelo',  'Mir Tur',            'consuelomirt',   '1980-12-31', '+34 622 001 010', '20000068J', 'FEMENINO',  TRUE,  '2025-01-19 10:00:00', '2026-04-11 17:00:00'),
('jaime.peralta@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Jaime',     'Peralta Bosch',      'jaimeperalta',   '1974-08-07', '+34 622 001 011', '20000069K', 'MASCULINO', TRUE,  '2025-02-01 08:00:00', '2026-04-10 08:30:00'),
('rosa.quijano@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Rosa',      'Quijano Ferri',      'rosaquijano',    '1981-03-24', '+34 622 001 012', '20000070L', 'FEMENINO',  TRUE,  '2025-02-02 09:00:00', '2026-04-09 09:00:00'),
('cesar.blanquer@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'César',     'Blanquer Ibáñez',    'cesarblanquer',  '1979-07-15', '+34 622 001 013', '20000071M', 'MASCULINO', TRUE,  '2025-02-03 10:00:00', '2026-04-08 10:00:00'),
('yolanda.navarro@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Yolanda',   'Navarro Bou',        'yolandanavarro', '1984-11-28', '+34 622 001 014', '20000072N', 'FEMENINO',  TRUE,  '2025-02-04 11:00:00', '2026-04-07 11:00:00'),
('fernando.galvez@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Fernando',  'Gálvez Sempere',     'fernandogalvez', '1976-01-12', '+34 622 001 015', '20000073O', 'MASCULINO', TRUE,  '2025-02-05 08:30:00', '2026-04-06 12:00:00'),
('amparo.marti@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Amparo',    'Martí Ortiz',        'amparomarti',    '1983-05-06', '+34 622 001 016', '20000074P', 'FEMENINO',  TRUE,  '2025-02-06 09:30:00', '2026-04-05 13:00:00'),
('pablo.cervera@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Pablo',     'Cervera Lluch',      'pablocervera',   '1980-09-20', '+34 622 001 017', '20000075Q', 'MASCULINO', TRUE,  '2025-02-07 10:30:00', '2026-04-04 14:00:00'),
('silvia.ribas@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Silvia',    'Ribas Domenech',     'silviaribas',    '1977-02-03', '+34 622 001 018', '20000076R', 'FEMENINO',  TRUE,  '2025-02-08 11:30:00', '2026-04-03 15:00:00'),
('jorge.peiro@gmail.com',        '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Jorge',     'Peiró Alapont',      'jorgepeiro',     '1985-06-17', '+34 622 001 019', '20000077S', 'MASCULINO', TRUE,  '2025-02-09 08:00:00', '2026-04-02 16:00:00'),
('carmen.lledo@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Carmen',    'Lledó Vidal',        'carmenlledo',    '1978-10-29', '+34 622 001 020', '20000078T', 'FEMENINO',  TRUE,  '2025-02-10 09:00:00', '2026-04-01 17:00:00'),
('antonio.segura@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Antonio',   'Segura Peris',       'antoniosegura',  '1982-03-08', '+34 622 001 021', '20000079U', 'MASCULINO', TRUE,  '2025-03-01 08:00:00', '2026-03-31 09:00:00'),
('beatriz.faus@gmail.com',       '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Beatriz',   'Faus Gregori',       'beatrizfaus',    '1976-07-21', '+34 622 001 022', '20000080V', 'FEMENINO',  TRUE,  '2025-03-02 09:00:00', '2026-03-30 10:00:00'),
('victor.sancho@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Víctor',    'Sancho Monfort',     'victorsancho',   '1984-12-14', '+34 622 001 023', '20000081W', 'MASCULINO', TRUE,  '2025-03-03 10:00:00', '2026-03-29 11:00:00'),
('lorena.castells@gmail.com',    '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Lorena',    'Castells Badia',     'lorenacastells', '1979-04-27', '+34 622 001 024', '20000082X', 'FEMENINO',  TRUE,  '2025-03-04 11:00:00', '2026-03-28 12:00:00'),
('manuel.escriba@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Manuel',    'Escribá Pitarch',    'manuelescrib',   '1981-08-10', '+34 622 001 025', '20000083Y', 'MASCULINO', TRUE,  '2025-03-05 08:30:00', '2026-03-27 13:00:00'),
('dolores.planelles@gmail.com',  '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Dolores',   'Planelles Cano',     'doloresplane',   '1975-01-23', '+34 622 001 026', '20000084Z', 'FEMENINO',  TRUE,  '2025-03-06 09:30:00', '2026-03-26 14:00:00'),
('rafael.fornes@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Rafael',    'Fornés Beltrán',     'rafaelfornes',   '1983-05-16', '+34 622 001 027', '20000085A', 'MASCULINO', TRUE,  '2025-03-07 10:30:00', '2026-03-25 15:00:00'),
('teresa.segarra@gmail.com',     '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'Teresa',    'Segarra Ferrer',     'teresasegarra',  '1980-09-03', '+34 622 001 028', '20000086B', 'FEMENINO',  TRUE,  '2025-03-08 11:30:00', '2026-03-24 16:00:00'),
('david.tortosa@gmail.com',      '$2a$10$WHUWMirDJuVxCokGxvVnoOal6ffgY4sjJ4EXuJtFtakLj/ygutbBe', 'David',     'Tortosa Giner',      'davidtortosa',   '1977-12-26', '+34 622 001 029', '20000087C', 'MASCULINO', TRUE,  '2025-03-09 08:00:00', '2026-03-23 17:00:00');


-- ============================================================
-- ROLES A USUARIOS NUEVOS
-- ============================================================
INSERT INTO rol_usuario (usuario_id, rol_id)
SELECT u.id_usuario, 2 FROM usuarios u
WHERE u.username IN (
    'sofiablanco','marcosfuentes','elenavega','pabloiglesias','claudiareyes',
    'ivancano','nereacampos','sergiomendez','raquelgimenez','hugoparra',
    'irenepascual','alejandrovidal','patriciamorin','diegocarrasco','veronicasantos',
    'antonioflores','beatrizleon','javiermunoz','amparogil','rubensanz',
    'miriamibanez','gonzalopeña','lauramolina','oscarherrera','carmenbravo',
    'nicolasg','adrianamoya','danielvarela','teresahurtado','jorgebolivar',
    'silviaquin','fernandona','cristinalozano','miguelpalacios','emmarodrigo',
    'victorespinosa','albadominguez','rauliborra','noeliacamacho','andresllopis',
    'luciatrillo','marioalcala','rocioserrano','carlosmendoza','juliaestrada',
    'pedrozamora','anacrespo','robertogallego','nuriacasado','albertomora'
);

INSERT INTO rol_usuario (usuario_id, rol_id)
SELECT u.id_usuario, 3 FROM usuarios u
WHERE u.username IN (
    'ramonsolano','anabustamante','josecastellano','pilaraguirre','luismontes',
    'estherbernal','ignaciocabello','monicapons','ernestotrujillo','consuelomirt',
    'jaimeperalta','rosaquijano','cesarblanquer','yolandanavarro','fernandogalvez',
    'amparomarti','pablocervera','silviaribas','jorgepeiro','carmenlledo',
    'antoniosegura','beatrizfaus','victorsancho','lorenacastells','manuelescrib',
    'doloresplane','rafaelfornes','teresasegarra','davidtortosa'
);


-- ============================================================
-- ESTUDIANTES
-- ============================================================
INSERT INTO estudiantes (usuario_id, grado_estudios_id)
SELECT u.id_usuario,
    CASE (ROW_NUMBER() OVER (ORDER BY u.id_usuario) % 6)
        WHEN 0 THEN 3  -- ESO
        WHEN 1 THEN 4  -- Bachillerato
        WHEN 2 THEN 5  -- Grado Medio
        WHEN 3 THEN 6  -- Grado Superior
        WHEN 4 THEN 7  -- Grado Universitario
        ELSE NULL
    END
FROM usuarios u
WHERE u.username IN (
    'sofiablanco','marcosfuentes','elenavega','pabloiglesias','claudiareyes',
    'ivancano','nereacampos','sergiomendez','raquelgimenez','hugoparra',
    'irenepascual','alejandrovidal','patriciamorin','diegocarrasco','veronicasantos',
    'antonioflores','beatrizleon','javiermunoz','amparogil','rubensanz',
    'miriamibanez','gonzalopeña','lauramolina','oscarherrera','carmenbravo',
    'nicolasg','adrianamoya','danielvarela','teresahurtado','jorgebolivar',
    'silviaquin','fernandona','cristinalozano','miguelpalacios','emmarodrigo',
    'victorespinosa','albadominguez','rauliborra','noeliacamacho','andresllopis',
    'luciatrillo','marioalcala','rocioserrano','carlosmendoza','juliaestrada',
    'pedrozamora','anacrespo','robertogallego','nuriacasado','albertomora'
);


-- ============================================================
-- CENTROS (30 centros: 12 verificados con gestor, 6 verificados sin gestor, 12 sin verificar)
-- ============================================================
INSERT INTO centros (nombre_comercial, codigo, descripcion, direccion, localidad, provincia_id, tipo, telefono, email, pagina_web, verificado, fecha_verificacion, tiene_gestor) VALUES

-- Verificados CON gestor (ids 4-15)
('IES Jaume I',                    '12000001', 'Instituto público con ciclos de informática.',            'Avinguda del Regne de València 15',    'Castelló de la Plana', (SELECT id_provincia FROM m_provincias WHERE nombre = 'Castellón'),     'PUBLICO',    '+34 964 100 001', 'info@iesjaume1.es',          'https://iesjaume1.es',          TRUE, '2025-03-01 10:00:00', TRUE),
('Academia Innova Madrid',         '28000002', 'Centro privado especializado en tecnología digital.',     'Calle Fuencarral 88',                  'Madrid',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Madrid'),        'PRIVADO',    '+34 910 200 002', 'hola@academiainnova.es',     'https://academiainnova.es',     TRUE, '2025-03-05 11:00:00', TRUE),
('CIFP Mediterráneo',              '03000003', 'Centro integrado con amplia oferta de ciclos.',           'Carrer de Colón 30',                   'Alacant',              (SELECT id_provincia FROM m_provincias WHERE nombre = 'Alicante'),      'PUBLICO',    '+34 965 100 003', 'contacto@cifpmed.es',        'https://cifpmediterraneo.es',   TRUE, '2025-03-10 12:00:00', TRUE),
('Escuela Negocios Vértice',       '28000004', 'Escuela privada con programas MBA y certificaciones.',    'Paseo de la Castellana 200',           'Madrid',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Madrid'),        'PRIVADO',    '+34 915 400 004', 'admisiones@vertice.es',      'https://vertice.es',            TRUE, '2025-03-15 09:00:00', TRUE),
('IES La Mar Xica',                '46000005', 'Instituto con especialización en turismo y hostelería.',  'Carrer del Mar 5',                     'Gandia',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Valencia'),      'PUBLICO',    '+34 962 100 005', 'secretaria@iesmarxica.es',   'https://iesmarxica.es',         TRUE, '2025-03-20 10:30:00', TRUE),
('Centro de Estudios Ágora',       '08000006', 'Centro concertado con formación en idiomas e informática.','Rambla de Catalunya 75',               'Barcelona',            (SELECT id_provincia FROM m_provincias WHERE nombre = 'Barcelona'),     'CONCERTADO', '+34 932 100 006', 'info@coleagora.es',          'https://coleagora.es',          TRUE, '2025-04-01 11:30:00', TRUE),
('FP Bilbao Profesional',          '48000007', 'Centro privado con ciclos de automoción y mecánica.',     'Gran Vía 45',                          'Bilbao',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Bizkaia'),       'PRIVADO',    '+34 944 200 007', 'fpbilbao@fpbilbao.es',       'https://fpbilbao.es',           TRUE, '2025-04-05 09:30:00', TRUE),
('IES Río Ebro',                   '50000008', 'Instituto con ciclos de química industrial.',             'Paseo de la Independencia 10',         'Zaragoza',             (SELECT id_provincia FROM m_provincias WHERE nombre = 'Zaragoza'),      'PUBLICO',    '+34 976 100 008', 'contacto@iesrioebro.es',     'https://iesrioebro.es',         TRUE, '2025-04-10 10:30:00', TRUE),
('Colegio Montserrat FP',          '08000009', 'Centro concertado con tradición en formación sanitaria.', 'Carrer de Muntaner 150',               'Barcelona',            (SELECT id_provincia FROM m_provincias WHERE nombre = 'Barcelona'),     'CONCERTADO', '+34 932 200 009', 'fp@montserratfp.es',         'https://montserratfp.es',       TRUE, '2025-04-15 11:30:00', TRUE),
('Academia TechSur',               '41000010', 'Academia privada de tecnología orientada a jóvenes.',     'Avenida de la Constitución 40',        'Sevilla',              (SELECT id_provincia FROM m_provincias WHERE nombre = 'Sevilla'),       'PRIVADO',    '+34 954 200 010', 'info@techsur.es',            'https://techsur.es',            TRUE, '2025-04-20 09:00:00', TRUE),
('CPIFP Aula Dei',                 '50000011', 'Centro público con ciclos de agraria y jardinería.',      'Ctra. de Montañana 921',               'Zaragoza',             (SELECT id_provincia FROM m_provincias WHERE nombre = 'Zaragoza'),      'PUBLICO',    '+34 976 200 011', 'secretaria@auladei.es',      'https://auladei.es',            TRUE, '2025-04-25 10:00:00', TRUE),
('Escuela de Arte Granada',        '18000012', 'Escuela de artes con ciclos de diseño gráfico y moda.',   'Calle Molinos 65',                     'Granada',              (SELECT id_provincia FROM m_provincias WHERE nombre = 'Granada'),       'PUBLICO',    '+34 958 200 012', 'info@eaigranada.es',         'https://eaigranada.es',         TRUE, '2025-05-01 11:00:00', TRUE),

-- Verificados SIN gestor (ids 16-21)
('Academia Canaria de FP',         '35000013', 'Centro privado en hostelería y turismo. Sin gestor.',     'Calle León y Castillo 200',            'Las Palmas',           (SELECT id_provincia FROM m_provincias WHERE nombre = 'Las Palmas'),    'PRIVADO',    '+34 928 200 013', 'contacto@acfp.es',           'https://acadcanarfp.es',        TRUE, '2025-05-05 09:30:00', FALSE),
('IES Laguna de Duero',            '47000014', 'Instituto público con ciclos de administración.',         'Calle Noria 3',                        'Laguna de Duero',      (SELECT id_provincia FROM m_provincias WHERE nombre = 'Valladolid'),    'PUBLICO',    '+34 983 100 014', 'ieslaguna@jcyl.es',          NULL,                            TRUE, '2025-05-10 10:30:00', FALSE),
('Centro Privado Altair',          '29000015', 'Centro privado integral con FP y bachillerato.',          'Avenida de la Paz 22',                 'Málaga',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Málaga'),        'PRIVADO',    '+34 952 300 015', 'secretaria@altair.es',       'https://altair.es',             TRUE, '2025-05-15 11:30:00', FALSE),
('CIFP Corona de Aragón',          '50000016', 'Centro integrado con ciclos de informática y sanidad.',   'Calle Corona de Aragón 35',            'Zaragoza',             (SELECT id_provincia FROM m_provincias WHERE nombre = 'Zaragoza'),      'PUBLICO',    '+34 976 300 016', 'cifp@coronaaragon.es',       'https://coronadearagon.es',     TRUE, '2025-05-20 09:00:00', FALSE),
('Concertado Sagrada Familia',     '08000017', 'Centro concertado con ciclos de administración.',         'Passeig de Gràcia 90',                 'Barcelona',            (SELECT id_provincia FROM m_provincias WHERE nombre = 'Barcelona'),     'CONCERTADO', '+34 934 100 017', 'fp@sagradafamilia.es',       'https://sagradafamiliafp.es',   TRUE, '2025-05-25 10:00:00', FALSE),
('FP Murcia Profesional',          '30000018', 'Centro privado con alta tasa de inserción laboral.',      'Avenida de la Libertad 50',            'Murcia',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Murcia'),        'PRIVADO',    '+34 968 200 018', 'info@fpmurcia.es',           'https://fpmurcia.es',           TRUE, '2025-06-01 11:00:00', FALSE),

-- Sin verificar (ids 22-33)
('IES Nuevo Horizonte',            '07000021', 'Instituto en proceso con ciclos de sanidad.',              'Calle Ancha 5',                        'Palma',                (SELECT id_provincia FROM m_provincias WHERE nombre = 'Islas Baleares'), 'PUBLICO',    '+34 971 100 021', 'info@iesnuevohorizonte.es',  NULL,                            FALSE, NULL, FALSE),
('Academia Future Skills',         '48000022', 'Centro privado emergente con soft skills.',               'Calle Ercilla 22',                     'Bilbao',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Bizkaia'),       'PRIVADO',    '+34 944 300 022', 'info@futureskills.es',       'https://futureskills.es',       FALSE, NULL, FALSE),
('Centro Formativo Atlántico',     '36000023', 'Centro privado con FP a distancia.',                      'Avenida Atlántica 80',                 'Vigo',                 (SELECT id_provincia FROM m_provincias WHERE nombre = 'Pontevedra'),    'PRIVADO',    '+34 986 200 023', 'contacto@cfatlantico.es',    'https://cfatlantico.es',        FALSE, NULL, FALSE),
('CIFP Ponent',                    '25000024', 'Centro público con ciclos industriales.',                 'Carrer de les Garrigues 10',           'Lleida',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Lleida'),        'PUBLICO',    '+34 973 100 024', 'secretaria@cifpponent.es',   NULL,                            FALSE, NULL, FALSE),
('Escuela Profesional Novatech',   '30000025', 'Centro privado especializado en nuevas tecnologías.',    'Avenida Juan Carlos I 30',             'Murcia',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Murcia'),        'PRIVADO',    '+34 968 300 025', 'info@novatech.es',           'https://novatech.es',           FALSE, NULL, FALSE),
('Academia Extremeña de FP',       '06000026', 'Centro privado en administración y contabilidad.',        'Avenida de Europa 12',                 'Badajoz',              (SELECT id_provincia FROM m_provincias WHERE nombre = 'Badajoz'),       'PRIVADO',    '+34 924 100 026', 'info@aefp.es',               NULL,                            FALSE, NULL, FALSE),
('IES Monte Aralar',               '20000027', 'Instituto público con ciclos de informática.',           'Paseo de Francia 18',                  'San Sebastián',        (SELECT id_provincia FROM m_provincias WHERE nombre = 'Gipuzkoa'),      'PUBLICO',    '+34 943 100 027', 'ies@montearalar.es',         'https://iesmontearalar.es',     FALSE, NULL, FALSE),
('Centro TIC Valencia',            '46000028', 'Centro privado de innovación digital.',                  'Calle Xúquer 15',                      'Valencia',             (SELECT id_provincia FROM m_provincias WHERE nombre = 'Valencia'),      'PRIVADO',    '+34 963 100 028', 'info@centroticval.es',       'https://centroticval.es',       FALSE, NULL, FALSE),
('IES Los Montes',                 '21000029', 'Instituto público recién autorizado.',                    'Carretera Sevilla km 5',               'Huelva',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Huelva'),        'PUBLICO',    '+34 959 100 029', 'contacto@ieslosmontes.es',   NULL,                            FALSE, NULL, FALSE),
('Academia Profesional Castellana','09000030', 'Centro privado de reciente creación.',                   'Plaza Mayor 8',                        'Burgos',               (SELECT id_provincia FROM m_provincias WHERE nombre = 'Burgos'),        'PRIVADO',    '+34 947 100 030', 'info@apcastellana.es',       'https://apcastellana.es',       FALSE, NULL, FALSE),
('CIPFP Ribera Alta',              '46000031', 'Centro público integrado de FP.',                        'Avenida del Río 25',                   'Requena',              (SELECT id_provincia FROM m_provincias WHERE nombre = 'Valencia'),      'PUBLICO',    '+34 962 300 031', 'cipfp@riberaalta.es',        'https://riberaaltafp.es',       FALSE, NULL, FALSE),
('Centro de Capacitación Laboral', '28000032', 'Centro privado de capacitación profesional.',            'Calle Industria 42',                   'Alcalá de Henares',    (SELECT id_provincia FROM m_provincias WHERE nombre = 'Madrid'),        'PRIVADO',    '+34 918 800 032', 'info@cclab.es',              'https://cclab.es',              FALSE, NULL, FALSE);


-- ============================================================
-- SOLICITUDES DE GESTIÓN (historial variado)
-- ============================================================
INSERT INTO solicitudes_gestion (usuario_id, centro_id, prueba_titularidad, estado, fecha_solicitud, fecha_resolucion)
SELECT u.id_usuario, c.id_centro,
    decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64'),
    sol.estado::VARCHAR,
    sol.fecha_solicitud,
    sol.fecha_resolucion
FROM (VALUES
    -- Aceptadas (centros ids 4-15 ya tienen gestor)
    ('ramonsolano',    4,  'ACEPTADA',  '2025-03-10 10:00:00'::TIMESTAMP, '2025-03-15 11:00:00'::TIMESTAMP),
    ('anabustamante',  5,  'ACEPTADA',  '2025-03-12 09:00:00'::TIMESTAMP, '2025-03-18 10:00:00'::TIMESTAMP),
    ('josecastellano', 6,  'ACEPTADA',  '2025-04-02 10:00:00'::TIMESTAMP, '2025-04-07 11:00:00'::TIMESTAMP),
    ('pilaraguirre',   7,  'ACEPTADA',  '2025-04-06 09:00:00'::TIMESTAMP, '2025-04-10 10:00:00'::TIMESTAMP),
    ('luismontes',     8,  'ACEPTADA',  '2025-04-11 10:00:00'::TIMESTAMP, '2025-04-16 11:00:00'::TIMESTAMP),
    ('estherbernal',   9,  'ACEPTADA',  '2025-04-16 09:00:00'::TIMESTAMP, '2025-04-21 10:00:00'::TIMESTAMP),
    ('ignaciocabello', 10, 'ACEPTADA',  '2025-04-21 10:00:00'::TIMESTAMP, '2025-04-26 11:00:00'::TIMESTAMP),
    ('monicapons',     11, 'ACEPTADA',  '2025-04-26 09:00:00'::TIMESTAMP, '2025-05-01 10:00:00'::TIMESTAMP),
    ('ernestotrujillo',12, 'ACEPTADA',  '2025-05-02 10:00:00'::TIMESTAMP, '2025-05-07 11:00:00'::TIMESTAMP),
    ('consuelomirt',   13, 'ACEPTADA',  '2025-05-06 09:00:00'::TIMESTAMP, '2025-05-11 10:00:00'::TIMESTAMP),
    ('jaimeperalta',   14, 'ACEPTADA',  '2025-05-11 10:00:00'::TIMESTAMP, '2025-05-16 11:00:00'::TIMESTAMP),
    ('rosaquijano',    15, 'ACEPTADA',  '2025-05-16 09:00:00'::TIMESTAMP, '2025-05-21 10:00:00'::TIMESTAMP),
    -- Rechazadas
    ('cesarblanquer',  16, 'RECHAZADA', '2025-05-21 10:00:00'::TIMESTAMP, '2025-05-26 11:00:00'::TIMESTAMP),
    ('yolandanavarro', 17, 'RECHAZADA', '2025-05-26 09:00:00'::TIMESTAMP, '2025-06-01 10:00:00'::TIMESTAMP),
    ('fernandogalvez', 18, 'RECHAZADA', '2025-06-02 10:00:00'::TIMESTAMP, '2025-06-07 11:00:00'::TIMESTAMP),
    -- Canceladas
    ('amparomarti',    19, 'CANCELADA', '2025-06-06 09:00:00'::TIMESTAMP, '2025-06-11 10:00:00'::TIMESTAMP),
    ('pablocervera',   20, 'CANCELADA', '2025-06-11 10:00:00'::TIMESTAMP, '2025-06-16 11:00:00'::TIMESTAMP),
    ('silviaribas',    21, 'CANCELADA', '2025-06-16 09:00:00'::TIMESTAMP, '2025-06-21 10:00:00'::TIMESTAMP),
    -- Pendientes (sin resolver)
    ('jorgepeiro',     22, 'PENDIENTE', '2026-04-15 10:00:00'::TIMESTAMP, NULL),
    ('carmenlledo',    23, 'PENDIENTE', '2026-04-16 09:00:00'::TIMESTAMP, NULL),
    ('antoniosegura',  24, 'PENDIENTE', '2026-04-17 10:00:00'::TIMESTAMP, NULL),
    ('beatrizfaus',    25, 'PENDIENTE', '2026-04-18 09:00:00'::TIMESTAMP, NULL)
) AS sol(username, centro_num, estado, fecha_solicitud, fecha_resolucion)
JOIN usuarios u ON u.username = sol.username
JOIN centros c  ON c.id_centro = sol.centro_num;


-- ============================================================
-- GESTORES CENTROS (asignación coherente con solicitudes aceptadas)
-- ============================================================
INSERT INTO gestores_centros (usuario_id, centro_id)
SELECT u.id_usuario, c.id_centro
FROM (VALUES
    ('ramonsolano',    4),  ('anabustamante',  5),  ('josecastellano', 6),
    ('pilaraguirre',   7),  ('luismontes',     8),  ('estherbernal',   9),
    ('ignaciocabello', 10), ('monicapons',     11), ('ernestotrujillo',12),
    ('consuelomirt',   13), ('jaimeperalta',   14), ('rosaquijano',    15)
) AS gc(username, centro_num)
JOIN usuarios u ON u.username = gc.username
JOIN centros c  ON c.id_centro = gc.centro_num;


-- ============================================================
-- FORMACIONES (40 formaciones en centros verificados)
-- ============================================================
INSERT INTO formaciones (centro_id, grado_estudios_requerido_id, nombre, titulo_oficial, descripcion, tipo_estudios, horario, duracion_horas, activa, precio, modalidad, fecha_inicio, fecha_fin) VALUES
(4,  4, 'Desarrollo de Aplicaciones Web (DAW)', 'Técnico Superior en DAW', 'Front-end, back-end con HTML, CSS, JS, PHP y Java.', 8, 'MANANA', 2000, TRUE,  0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(4,  3, 'Sistemas Microinformáticos y Redes (SMR)', 'Técnico en SMR', 'Mantenimiento de equipos e instalación de redes.', 7, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(5,  4, 'Administración de Sistemas (ASIR)', 'Técnico Superior en ASIR', 'Gestión de servidores, redes y seguridad.', 8, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(5,  4, 'Gestión Administrativa (GA)', 'Técnico Superior en GA', 'Contabilidad, fiscalidad y laboral de empresas.', 8, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(6,  NULL, 'Inglés de Negocios B2-C1', NULL, 'Comunicación empresarial y negociación en inglés.', 10, 'FLEXIBLE', 150, TRUE, 390.00, 'SEMIPRESENCIAL', '2026-01-10', '2026-06-30'),
(6,  NULL, 'Diseño Gráfico Adobe Creative Suite', NULL, 'Photoshop, Illustrator, InDesign y motion graphics.', 14, 'TARDE', 200, TRUE, 550.00, 'PRESENCIAL', '2026-02-01', '2026-07-31'),
(7,  3, 'Mantenimiento de Vehículos', 'Técnico en Electromecánica de Vehículos', 'Diagnóstico y reparación de vehículos.', 7, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(7,  4, 'Mecatrónica Industrial', 'Técnico Superior en Mecatrónica Industrial', 'Robótica, automatización y sistemas integrados.', 8, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(8,  4, 'Laboratorio de Análisis y Control de Calidad', 'Técnico Superior en Laboratorio', 'Análisis químicos e instrumentales.', 8, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(8,  3, 'Química Industrial', 'Técnico en Operaciones de Laboratorio', 'Procesos de laboratorio e industrial.', 7, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(9,  NULL, 'Auxiliar de Enfermería', 'Técnico en Cuidados Auxiliares de Enfermería', 'Atención sanitaria y técnicas de enfermería.', 7, 'MANANA', 1400, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2026-06-15'),
(9,  4, 'Documentación Sanitaria', 'Técnico Superior en Documentación Sanitaria', 'Gestión de información clínica y codificación.', 8, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(10, NULL, 'Bootcamp Full Stack JavaScript', NULL, 'Node.js, React, MongoDB y despliegue en nube.', 14, 'FLEXIBLE', 900, TRUE, 4500.00, 'PRESENCIAL', '2026-03-01', '2026-09-30'),
(10, NULL, 'Inteligencia Artificial Aplicada', NULL, 'Machine learning, redes neuronales, Python.', 14, 'FLEXIBLE', 300, TRUE, 1200.00, 'SEMIPRESENCIAL', '2026-04-01', '2026-09-01'),
(11, 4, 'Producción Agroecológica', 'Técnico Superior en Paisajismo y Medio Rural', 'Técnicas agroecológicas y sostenibilidad.', 8, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(11, 3, 'Jardinería y Paisajismo', 'Técnico en Jardinería y Floristería', 'Diseño y mantenimiento de jardines.', 7, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(12, 3, 'Cerámica Artística', 'Técnico de Artes Plásticas en Cerámica', 'Modelado, cocción y decoración de piezas.', 11, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(12, 4, 'Diseño Gráfico Superior', 'Técnico Superior en Diseño Gráfico', 'Diseño editorial y comunicación visual.', 11, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(13, 3, 'Servicios de Restauración', 'Técnico en Servicios en Restauración', 'Sala, bar, coctelería y atención al cliente.', 7, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(13, 4, 'Dirección de Cocina', 'Técnico Superior en Dirección de Cocina', 'Gastronomía avanzada y gestión de cocina.', 8, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(14, 4, 'Administración y Finanzas', 'Técnico Superior en Administración y Finanzas', 'Contabilidad, fiscalidad y nóminas.', 8, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(14, NULL, 'Certificación Microsoft AZ-900', NULL, 'Fundamentos de cloud computing con Azure.', 14, 'FLEXIBLE', 40, TRUE, 299.00, 'DISTANCIA', '2026-01-01', '2026-12-31'),
(15, NULL, 'Preparación EBAU / Selectividad', NULL, 'Repaso intensivo de materias de Bachillerato.', 5, 'MANANA', 600, TRUE, 1800.00, 'PRESENCIAL', '2025-09-15', '2026-06-15'),
(15, NULL, 'Coaching Profesional', NULL, 'Liderazgo, comunicación asertiva y marca personal.', 14, 'FLEXIBLE', 60, TRUE, 450.00, 'SEMIPRESENCIAL', '2026-02-15', '2026-05-15'),
(16, 4, 'Educación Infantil', 'Técnico Superior en Educación Infantil', 'Desarrollo evolutivo y didáctica.', 8, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(16, 4, 'Integración Social', 'Técnico Superior en Integración Social', 'Atención a personas en riesgo de exclusión.', 8, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(17, NULL, 'MBA en Dirección de Empresas', NULL, 'Estrategia empresarial, finanzas y marketing.', 15, 'NOCHE', 600, TRUE, 5800.00, 'SEMIPRESENCIAL', '2026-01-15', '2026-12-15'),
(17, 4, 'Marketing y Publicidad', 'Técnico Superior en Marketing y Publicidad', 'Estrategia de marketing y publicidad digital.', 8, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(18, 3, 'Peluquería y Cosmética', 'Técnico en Peluquería y Cosmética Capilar', 'Corte, color y tratamientos capilares.', 7, 'MANANA', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(18, 4, 'Estética Integral y Bienestar', 'Técnico Superior en Estética Integral y Bienestar', 'Tratamientos estéticos, depilación y masajes.', 8, 'TARDE', 2000, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2027-06-15'),
(19, NULL, 'Certificación AWS Cloud Practitioner', NULL, 'Fundamentos de Amazon Web Services.', 14, 'FLEXIBLE', 40, TRUE, 349.00, 'DISTANCIA', '2026-01-01', '2026-12-31'),
(19, NULL, 'Ciberseguridad para Empresas', NULL, 'Pentesting, análisis de vulnerabilidades y RGPD.', 14, 'NOCHE', 200, TRUE, 980.00, 'SEMIPRESENCIAL', '2026-03-01', '2026-07-31'),
(20, NULL, 'Técnico en Emergencias Sanitarias', 'Técnico en Emergencias Sanitarias', 'Soporte vital y atención pre-hospitalaria.', 7, 'MANANA', 1400, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2026-06-15'),
(20, NULL, 'Atención a Personas Dependientes', 'Técnico en Atención a Personas en Situación de Dependencia', 'Cuidados a mayores y discapacitados.', 7, 'TARDE', 1400, TRUE, 0.00, 'PRESENCIAL', '2025-09-15', '2026-06-15'),
(21, 4, 'Programación y Robótica Educativa', NULL, 'Arduino, Scratch y Python educativo.', 14, 'FLEXIBLE', 120, TRUE, 320.00, 'SEMIPRESENCIAL', '2026-02-01', '2026-06-30'),
(4,  NULL, 'Big Data e Inteligencia Artificial', NULL, 'Hadoop, Spark, Power BI y ciencia de datos.', 14, 'NOCHE', 250, FALSE, 1100.00, 'DISTANCIA', '2024-03-15', '2024-09-15'),
(5,  NULL, 'Community Manager y Redes Sociales', NULL, 'Estrategia de contenidos y gestión digital.', 14, 'FLEXIBLE', 100, FALSE, 420.00, 'DISTANCIA', '2024-01-01', '2024-12-31'),
(6,  4, 'Comercio Internacional', 'Técnico Superior en Comercio Internacional', 'Exportación, incoterms y documentación.', 8, 'MANANA', 2000, FALSE, 0.00, 'PRESENCIAL', '2024-09-15', '2026-06-15');


-- ============================================================
-- SOLICITUDES DE FORMACIÓN (inscripciones variadas)
-- ============================================================
INSERT INTO solicitudes_formacion (estudiante_id, formacion_id, estado, fecha_solicitud, fecha_respuesta)
SELECT e.id_estudiante, f.id_formacion, sf.estado::VARCHAR, sf.fecha_solicitud, sf.fecha_respuesta
FROM (VALUES
    (5,  1,  'ACEPTADA',  '2025-09-20 09:00:00'::TIMESTAMP, '2025-09-28 10:00:00'::TIMESTAMP),
    (5,  2,  'PENDIENTE', '2026-01-15 10:00:00'::TIMESTAMP, NULL),
    (6,  3,  'ACEPTADA',  '2025-09-18 10:00:00'::TIMESTAMP, '2025-09-25 11:00:00'::TIMESTAMP),
    (6,  4,  'RECHAZADA', '2026-01-20 10:00:00'::TIMESTAMP, '2026-01-25 12:00:00'::TIMESTAMP),
    (7,  5,  'ACEPTADA',  '2025-10-01 09:00:00'::TIMESTAMP, '2025-10-10 10:00:00'::TIMESTAMP),
    (7,  6,  'PENDIENTE', '2026-02-01 09:00:00'::TIMESTAMP, NULL),
    (8,  7,  'ACEPTADA',  '2025-09-22 11:00:00'::TIMESTAMP, '2025-09-30 10:00:00'::TIMESTAMP),
    (8,  8,  'CANCELADA', '2026-02-10 10:00:00'::TIMESTAMP, '2026-02-11 09:00:00'::TIMESTAMP),
    (9,  9,  'ACEPTADA',  '2025-10-05 09:00:00'::TIMESTAMP, '2025-10-12 10:00:00'::TIMESTAMP),
    (9,  10, 'PENDIENTE', '2026-03-01 10:00:00'::TIMESTAMP, NULL),
    (10, 11, 'ACEPTADA',  '2025-09-16 10:00:00'::TIMESTAMP, '2025-09-23 11:00:00'::TIMESTAMP),
    (10, 12, 'RECHAZADA', '2026-03-10 10:00:00'::TIMESTAMP, '2026-03-15 11:00:00'::TIMESTAMP),
    (11, 13, 'ACEPTADA',  '2025-10-08 10:00:00'::TIMESTAMP, '2025-10-15 11:00:00'::TIMESTAMP),
    (11, 14, 'PENDIENTE', '2026-04-01 10:00:00'::TIMESTAMP, NULL),
    (12, 15, 'ACEPTADA',  '2025-10-15 10:00:00'::TIMESTAMP, '2025-10-22 11:00:00'::TIMESTAMP),
    (12, 16, 'CANCELADA', '2026-02-20 10:00:00'::TIMESTAMP, '2026-02-21 09:00:00'::TIMESTAMP),
    (13, 17, 'ACEPTADA',  '2025-11-01 10:00:00'::TIMESTAMP, '2025-11-10 11:00:00'::TIMESTAMP),
    (13, 18, 'PENDIENTE', '2026-03-15 10:00:00'::TIMESTAMP, NULL),
    (14, 19, 'ACEPTADA',  '2025-11-15 10:00:00'::TIMESTAMP, '2025-11-22 11:00:00'::TIMESTAMP),
    (14, 20, 'RECHAZADA', '2026-01-08 10:00:00'::TIMESTAMP, '2026-01-15 11:00:00'::TIMESTAMP)
) AS sf(est_id, form_id, estado, fecha_solicitud, fecha_respuesta)
JOIN estudiantes e ON e.id_estudiante = sf.est_id
JOIN formaciones  f ON f.id_formacion  = sf.form_id;


-- ============================================================
-- VALORACIONES
-- ============================================================
INSERT INTO valoraciones (estudiante_id, formacion_id, estrellas, comentario, fecha)
SELECT e.id_estudiante, f.id_formacion, v.estrellas, v.comentario, v.fecha
FROM (VALUES
    (5,  1,  5, 'Excelente ciclo de DAW. Infraestructura de prácticas moderna.',                    '2025-12-15 10:00:00'::TIMESTAMP),
    (6,  3,  4, 'ASIR muy completo. Solo mejoraría la actualización de algunos contenidos.',       '2026-02-10 11:00:00'::TIMESTAMP),
    (7,  5,  5, 'Inglés de negocios muy práctico. Profesores nativos.',                            '2026-01-20 12:00:00'::TIMESTAMP),
    (8,  7,  3, 'Mantenimiento vehículos correcto pero las instalaciones necesitan renovación.',   '2026-01-25 13:00:00'::TIMESTAMP),
    (9,  9,  5, 'Laboratorio con equipamiento excelente. Muy satisfecho.',                         '2026-02-20 09:00:00'::TIMESTAMP),
    (10, 11, 4, 'Auxiliar enfermería bien estructurado. Buen equilibrio teoría-práctica.',         '2026-02-28 10:00:00'::TIMESTAMP),
    (11, 13, 5, 'Bootcamp Full Stack intenso pero muy efectivo. Obtuve trabajo.',                 '2025-12-20 11:00:00'::TIMESTAMP),
    (12, 15, 4, 'Agroecología con un campo de prácticas increíble.',                              '2026-01-10 12:00:00'::TIMESTAMP),
    (13, 17, 5, 'Cerámica artística transformadora. Aprendí técnicas avanzadas.',                 '2025-12-28 11:00:00'::TIMESTAMP),
    (14, 19, 4, 'Servicios restauración bien impartido. Prácticas en restaurante real.',          '2026-01-15 09:00:00'::TIMESTAMP)
) AS v(est_id, form_id, estrellas, comentario, fecha)
JOIN estudiantes  e ON e.id_estudiante = v.est_id
JOIN formaciones  f ON f.id_formacion  = v.form_id;


-- ============================================================
-- FORMACIONES FAVORITAS
-- ============================================================
INSERT INTO formaciones_favoritas (estudiante_id, formacion_id)
SELECT e.id_estudiante, f.id_formacion
FROM (VALUES
    (5, 1), (5, 13), (5, 5),
    (6, 3), (6, 4), (6, 6),
    (7, 5), (7, 6),
    (8, 7), (8, 8),
    (9, 9), (9, 10),
    (10, 11), (10, 12),
    (11, 13), (11, 14),
    (12, 15), (12, 16),
    (13, 17), (13, 18),
    (14, 19), (14, 20)
) AS fav(est_id, form_id)
JOIN estudiantes  e ON e.id_estudiante = fav.est_id
JOIN formaciones  f ON f.id_formacion  = fav.form_id;
