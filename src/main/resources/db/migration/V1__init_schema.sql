-- V1: Esquema inicial de EncuentraFormación
-- Contiene: estructura de tablas + datos de referencia (tablas maestras, roles)
-- Datos de prueba: ver db/sample/datos_ejemplo.sql (ejecución manual)

SET search_path TO encuentra_formacion;

-- ─────────────────────────────────────────────────────────────────
-- FUNCIÓN DE TRIGGER: actualiza fecha_modificacion automáticamente
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_fecha_modificacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_modificacion = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ─────────────────────────────────────────────────────────────────
-- TABLA MAESTRA: m_grado_estudios
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE m_grado_estudios (
    id_grado_estudios BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre            VARCHAR(50) NOT NULL UNIQUE
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA MAESTRA: m_roles
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE m_roles (
    id_rol BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA MAESTRA: m_provincias
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE m_provincias (
    id_provincia BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       VARCHAR(80) NOT NULL UNIQUE,
    codigo_ine   CHAR(2)     NOT NULL UNIQUE
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA: usuarios
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE usuarios (
    id_usuario         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email              VARCHAR(150) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    nombre             VARCHAR(100) NOT NULL,
    apellidos          VARCHAR(150) NOT NULL,
    username           VARCHAR(50)  NOT NULL UNIQUE,
    fecha_nacimiento   DATE,
    telefono           VARCHAR(20),
    dni                VARCHAR(9)   UNIQUE,
    sexo               VARCHAR(10)  CHECK (sexo IN ('MASCULINO', 'FEMENINO', 'OTRO')),
    ultima_conexion    TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    fecha_alta         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo             BOOLEAN      DEFAULT TRUE
);

CREATE TRIGGER trg_usuarios_fecha_modificacion
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION update_fecha_modificacion();

-- ─────────────────────────────────────────────────────────────────
-- TABLA: rol_usuario (N:M)
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE rol_usuario (
    usuario_id BIGINT NOT NULL,
    rol_id     BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_rol_usuario_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_rol_usuario_rol FOREIGN KEY (rol_id)
        REFERENCES m_roles(id_rol) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA: estudiantes (extiende usuarios)
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE estudiantes (
    id_estudiante     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id        BIGINT NOT NULL UNIQUE,
    grado_estudios_id BIGINT,
    CONSTRAINT fk_estudiantes_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_estudiantes_grado_estudios FOREIGN KEY (grado_estudios_id)
        REFERENCES m_grado_estudios(id_grado_estudios) ON DELETE SET NULL
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA: centros
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE centros (
    id_centro          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_comercial   VARCHAR(200) NOT NULL,
    cif                VARCHAR(20)  NOT NULL UNIQUE,
    descripcion        TEXT,
    direccion          VARCHAR(255) NOT NULL,
    localidad          VARCHAR(100) NOT NULL,
    provincia_id       BIGINT       NOT NULL,
    tipo               VARCHAR(15)  NOT NULL CHECK (tipo IN ('PUBLICO', 'PRIVADO', 'CONCERTADO')),
    telefono           VARCHAR(20),
    email              VARCHAR(150) UNIQUE,
    pagina_web         VARCHAR(255),
    verificado         BOOLEAN      DEFAULT FALSE,
    fecha_verificacion TIMESTAMP,
    tiene_gestor       BOOLEAN      DEFAULT FALSE,
    fecha_alta         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    CONSTRAINT fk_centros_provincia FOREIGN KEY (provincia_id)
        REFERENCES m_provincias(id_provincia) ON DELETE RESTRICT
);

CREATE TRIGGER trg_centros_fecha_modificacion
    BEFORE UPDATE ON centros
    FOR EACH ROW
    EXECUTE FUNCTION update_fecha_modificacion();

-- ─────────────────────────────────────────────────────────────────
-- TABLA: solicitudes_gestion
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE solicitudes_gestion (
    id_solicitud       BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id         BIGINT      NOT NULL,
    centro_id          BIGINT      NOT NULL,
    prueba_titularidad BYTEA       NOT NULL,
    estado             VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE'
                           CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA')),
    fecha_solicitud    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion   TIMESTAMP,
    CONSTRAINT fk_solicitudes_gestion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_solicitudes_gestion_centro FOREIGN KEY (centro_id)
        REFERENCES centros(id_centro) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA: gestores_centros (N:M)
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE gestores_centros (
    usuario_id        BIGINT    NOT NULL,
    centro_id         BIGINT    NOT NULL,
    fecha_asignacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, centro_id),
    CONSTRAINT fk_gestores_centros_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_gestores_centros_centro FOREIGN KEY (centro_id)
        REFERENCES centros(id_centro) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA: formaciones
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE formaciones (
    id_formacion               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    centro_id                  BIGINT       NOT NULL,
    grado_estudios_requerido_id BIGINT,
    nombre                     VARCHAR(200) NOT NULL,
    titulo_oficial             VARCHAR(255),
    descripcion                TEXT,
    tipo_estudios              BIGINT,
    horario                    VARCHAR(10)  NOT NULL CHECK (horario IN ('MANANA', 'TARDE', 'NOCHE', 'FLEXIBLE')),
    duracion_horas             INT,
    activa                     BOOLEAN      DEFAULT TRUE,
    precio                     DECIMAL(10,2),
    modalidad                  VARCHAR(15)  NOT NULL CHECK (modalidad IN ('PRESENCIAL', 'SEMIPRESENCIAL', 'DISTANCIA')),
    fecha_inicio               DATE,
    fecha_fin                  DATE,
    fecha_alta                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion         TIMESTAMP,
    CONSTRAINT fk_formaciones_grado_estudios_requerido FOREIGN KEY (grado_estudios_requerido_id)
        REFERENCES m_grado_estudios(id_grado_estudios) ON DELETE SET NULL,
    CONSTRAINT fk_formaciones_tipo_estudios FOREIGN KEY (tipo_estudios)
        REFERENCES m_grado_estudios(id_grado_estudios) ON DELETE SET NULL,
    CONSTRAINT fk_formaciones_centro FOREIGN KEY (centro_id)
        REFERENCES centros(id_centro) ON DELETE CASCADE
);

CREATE TRIGGER trg_formaciones_fecha_modificacion
    BEFORE UPDATE ON formaciones
    FOR EACH ROW
    EXECUTE FUNCTION update_fecha_modificacion();

-- ─────────────────────────────────────────────────────────────────
-- TABLA: solicitudes_formacion
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE solicitudes_formacion (
    id_solicitud    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estudiante_id   BIGINT      NOT NULL,
    formacion_id    BIGINT      NOT NULL,
    estado          VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado IN ('PENDIENTE', 'CANCELADA', 'ACEPTADA', 'RECHAZADA')),
    fecha_solicitud TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_respuesta TIMESTAMP,
    UNIQUE (estudiante_id, formacion_id),
    CONSTRAINT fk_solicitudes_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_solicitudes_formacion FOREIGN KEY (formacion_id)
        REFERENCES formaciones(id_formacion) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA: valoraciones
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE valoraciones (
    id_valoracion BIGINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    estudiante_id BIGINT    NOT NULL,
    formacion_id  BIGINT    NOT NULL,
    estrellas     INT       NOT NULL CHECK (estrellas >= 1 AND estrellas <= 5),
    comentario    TEXT,
    fecha         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_valoraciones_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_valoraciones_formacion FOREIGN KEY (formacion_id)
        REFERENCES formaciones(id_formacion) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────────
-- TABLA: formaciones_favoritas (N:M)
-- ─────────────────────────────────────────────────────────────────
CREATE TABLE formaciones_favoritas (
    estudiante_id BIGINT NOT NULL,
    formacion_id  BIGINT NOT NULL,
    PRIMARY KEY (estudiante_id, formacion_id),
    CONSTRAINT fk_formaciones_favoritas_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_formaciones_favoritas_formacion FOREIGN KEY (formacion_id)
        REFERENCES formaciones(id_formacion) ON DELETE CASCADE
);

-- ═════════════════════════════════════════════════════════════════
-- DATOS DE REFERENCIA
-- ═════════════════════════════════════════════════════════════════

-- Niveles de estudios
INSERT INTO m_grado_estudios (id_grado_estudios, nombre) OVERRIDING SYSTEM VALUE VALUES
    (1,  'Sin estudios'),
    (2,  'Primaria'),
    (3,  'ESO'),
    (4,  'Bachillerato'),
    (5,  'Grado Medio'),
    (6,  'Grado Superior'),
    (7,  'Universidad'),
    (8,  'Máster'),
    (9,  'Curso'),
    (10, 'Doctorado');

-- Roles de usuario
INSERT INTO m_roles (nombre) VALUES
    ('ADMIN'),
    ('ESTUDIANTE'),
    ('GESTOR_CENTRO');

-- Provincias de España (códigos INE oficiales)
INSERT INTO m_provincias (nombre, codigo_ine) VALUES
    ('Álava',                    '01'),
    ('Albacete',                 '02'),
    ('Alicante',                 '03'),
    ('Almería',                  '04'),
    ('Ávila',                    '05'),
    ('Badajoz',                  '06'),
    ('Islas Baleares',           '07'),
    ('Barcelona',                '08'),
    ('Burgos',                   '09'),
    ('Cáceres',                  '10'),
    ('Cádiz',                    '11'),
    ('Castellón',                '12'),
    ('Ciudad Real',              '13'),
    ('Córdoba',                  '14'),
    ('A Coruña',                 '15'),
    ('Cuenca',                   '16'),
    ('Girona',                   '17'),
    ('Granada',                  '18'),
    ('Guadalajara',              '19'),
    ('Gipuzkoa',                 '20'),
    ('Huelva',                   '21'),
    ('Huesca',                   '22'),
    ('Jaén',                     '23'),
    ('León',                     '24'),
    ('Lleida',                   '25'),
    ('La Rioja',                 '26'),
    ('Lugo',                     '27'),
    ('Madrid',                   '28'),
    ('Málaga',                   '29'),
    ('Murcia',                   '30'),
    ('Navarra',                  '31'),
    ('Ourense',                  '32'),
    ('Asturias',                 '33'),
    ('Palencia',                 '34'),
    ('Las Palmas',               '35'),
    ('Pontevedra',               '36'),
    ('Salamanca',                '37'),
    ('Santa Cruz de Tenerife',   '38'),
    ('Cantabria',                '39'),
    ('Segovia',                  '40'),
    ('Sevilla',                  '41'),
    ('Soria',                    '42'),
    ('Tarragona',                '43'),
    ('Teruel',                   '44'),
    ('Toledo',                   '45'),
    ('Valencia',                 '46'),
    ('Valladolid',               '47'),
    ('Bizkaia',                  '48'),
    ('Zamora',                   '49'),
    ('Zaragoza',                 '50'),
    ('Ceuta',                    '51'),
    ('Melilla',                  '52');
