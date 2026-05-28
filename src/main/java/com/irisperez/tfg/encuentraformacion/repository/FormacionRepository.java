package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.enums.HorarioFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.repository.projection.CentroFormacionCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormacionRepository extends JpaRepository<Formacion, Long>, JpaSpecificationExecutor<Formacion> {

    // Busca una formación activa por ID, para operaciones internas del gestor
    Optional<Formacion> findByIdAndActivaTrue(Long id);

    // Busca por UUID público cargando centro y tipo de estudios en una sola query
    @Query("SELECT f FROM Formacion f JOIN FETCH f.centro c " +
           "LEFT JOIN FETCH f.tipoEstudios " +
           "WHERE f.uuid = :uuid AND f.activa = true AND c.verificado = true")
    Optional<Formacion> findByUuid(@Param("uuid") UUID uuid);

    // Lista las formaciones activas de un centro concreto, con el centro ya cargado
    // para evitar queries adicionales al serializar
    @Query("SELECT f FROM Formacion f " +
           "JOIN FETCH f.centro c " +
           "LEFT JOIN FETCH f.tipoEstudios " +
           "WHERE c.id = :centroId AND f.activa = true " +
           "ORDER BY f.fechaAlta DESC")
    List<Formacion> findActivasByCentroIdConCentro(@Param("centroId") Long centroId);

    // Versión paginada — centroId ya resuelto para evitar subconsulta al paginar
    @Query(value = "SELECT f FROM Formacion f JOIN FETCH f.centro c LEFT JOIN FETCH f.tipoEstudios " +
                   "WHERE c.id = :centroId AND f.activa = true ORDER BY f.fechaAlta DESC",
           countQuery = "SELECT COUNT(f) FROM Formacion f WHERE f.centro.id = :centroId AND f.activa = true")
    Page<Formacion> findActivasByCentroIdPageable(@Param("centroId") Long centroId, Pageable pageable);

    @Query("SELECT f.centro.id AS centroId, COUNT(f) AS total FROM Formacion f WHERE f.centro.id IN :centroIds AND f.activa = true GROUP BY f.centro.id")
    List<CentroFormacionCountProjection> countActivasByCentroIds(@Param("centroIds") List<Long> centroIds);

    // Buscador público con todos los filtros opcionales, ordenado por valoración media
    @Query("""
            SELECT f FROM Formacion f
            JOIN f.centro c
            LEFT JOIN f.tipoEstudios te
            WHERE f.activa = true
              AND c.verificado = true
              AND (:filtrarNombre        = false OR unaccent(LOWER(f.nombre))        LIKE CONCAT('%', :nombre, '%'))
              AND (:filtrarTituloOficial = false OR unaccent(LOWER(f.tituloOficial)) LIKE CONCAT('%', :tituloOficial, '%'))
              AND (:filtrarLocalidad     = false OR unaccent(LOWER(c.localidad))     LIKE CONCAT('%', :localidad, '%'))
              AND (:modalidad      IS NULL OR f.modalidad = :modalidad)
              AND (:provinciaId    IS NULL OR c.provincia.id = :provinciaId)
              AND (:horario        IS NULL OR f.horario = :horario)
              AND (:tipoEstudiosId IS NULL OR te.id = :tipoEstudiosId)
              AND (:tipoCentro     IS NULL OR c.tipo = :tipoCentro)
              AND (:soloGratuitas  = false OR f.precio IS NULL OR f.precio = 0)
              AND (:precioMin      IS NULL OR f.precio >= :precioMin)
              AND (:precioMax      IS NULL OR f.precio <= :precioMax)
              AND (:fechaInicioDesde IS NULL OR f.fechaInicio >= :fechaInicioDesde)
            ORDER BY (
                SELECT COALESCE(AVG(v.estrellas), 0)
                FROM Valoracion v
                WHERE v.formacion.id = f.id
            ) DESC, (
                SELECT COUNT(v)
                FROM Valoracion v
                WHERE v.formacion.id = f.id
            ) DESC
            """)
    Page<Formacion> findOrdenadosPorValoracion(
            @Param("filtrarNombre")        boolean filtrarNombre,
            @Param("nombre")               String nombre,
            @Param("filtrarTituloOficial") boolean filtrarTituloOficial,
            @Param("tituloOficial")        String tituloOficial,
            @Param("filtrarLocalidad")     boolean filtrarLocalidad,
            @Param("localidad")            String localidad,
            @Param("modalidad")            ModalidadFormacion modalidad,
            @Param("provinciaId")          Long provinciaId,
            @Param("horario")              HorarioFormacion horario,
            @Param("tipoEstudiosId")       Long tipoEstudiosId,
            @Param("tipoCentro")           TipoCentro tipoCentro,
            @Param("soloGratuitas")        boolean soloGratuitas,
            @Param("precioMin")            BigDecimal precioMin,
            @Param("precioMax")            BigDecimal precioMax,
            @Param("fechaInicioDesde")     LocalDate fechaInicioDesde,
            Pageable pageable);

    // Para listar formaciones del centro del gestor (activas e inactivas)
    Page<Formacion> findByCentroId(Long centroId, Pageable pageable);

    // Para validar propiedad antes de editar/desactivar
    Optional<Formacion> findByIdAndCentroId(Long id, Long centroId);

    // Contar solicitudes pendientes por formación (para el DTO)
    @Query("SELECT COUNT(s) FROM SolicitudFormacion s WHERE s.formacion.id = :formacionId AND s.estado = 'PENDIENTE'")
    long countSolicitudesPendientesByFormacionId(@Param("formacionId") Long formacionId);
}
