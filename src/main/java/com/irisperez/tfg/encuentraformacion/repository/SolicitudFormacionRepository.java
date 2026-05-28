package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudFormacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface SolicitudFormacionRepository extends JpaRepository<SolicitudFormacion, Long> {

    @Query(value = "SELECT s FROM SolicitudFormacion s " +
                   "JOIN FETCH s.formacion f " +
                   "JOIN FETCH f.centro c " +
                   "WHERE s.estudiante.id = :estId " +
                   "AND (:estado IS NULL OR s.estado = :estado) " +
                   "ORDER BY s.fechaSolicitud DESC",
           countQuery = "SELECT COUNT(s) FROM SolicitudFormacion s " +
                        "WHERE s.estudiante.id = :estId " +
                        "AND (:estado IS NULL OR s.estado = :estado)")
    Page<SolicitudFormacion> findByEstudianteIdConFormacion(
        @Param("estId") Long estId,
        @Param("estado") EstadoSolicitud estado,
        Pageable pageable);

    @Query("SELECT s FROM SolicitudFormacion s " +
           "JOIN FETCH s.formacion f JOIN FETCH f.centro c " +
           "WHERE s.id = :id AND s.estudiante.usuario.id = :usuarioId")
    Optional<SolicitudFormacion> findByIdAndEstudiante_UsuarioId(
        @Param("id") Long id,
        @Param("usuarioId") Long usuarioId);

    boolean existsByEstudianteIdAndFormacionIdAndEstado(Long estId, Long formId, EstadoSolicitud estado);

    @Query("SELECT s FROM SolicitudFormacion s " +
           "JOIN FETCH s.formacion f JOIN FETCH f.centro c " +
           "WHERE s.estudiante.id = :estId AND f.uuid = :uuid " +
           "ORDER BY s.fechaSolicitud DESC")
    java.util.List<SolicitudFormacion> findByEstudianteIdAndFormacionUuid(
        @Param("estId") Long estId,
        @Param("uuid") java.util.UUID uuid);

    // Listar solicitudes del centro con filtro opcional de estado
    @Query(value = "SELECT s FROM SolicitudFormacion s " +
           "JOIN FETCH s.estudiante e JOIN FETCH e.usuario " +
           "JOIN FETCH s.formacion f " +
           "WHERE f.centro.id = :centroId " +
           "AND (:estado IS NULL OR s.estado = :estado)",
           countQuery = "SELECT COUNT(s) FROM SolicitudFormacion s " +
           "WHERE s.formacion.centro.id = :centroId " +
           "AND (:estado IS NULL OR s.estado = :estado)")
    Page<SolicitudFormacion> findByCentroId(
        @Param("centroId") Long centroId,
        @Param("estado") EstadoSolicitud estado,
        Pageable pageable);

    @Query(value = """
        SELECT s FROM SolicitudFormacion s
        JOIN FETCH s.estudiante e JOIN FETCH e.usuario u
        JOIN FETCH s.formacion f
        WHERE f.centro.id = :centroId
          AND (:formacionId IS NULL OR f.id = :formacionId)
          AND (:#{#estados == null || #estados.isEmpty()} = true OR s.estado IN :estados)
          AND (:nombre = '' OR LOWER(CONCAT(u.nombre, ' ', u.apellidos)) LIKE LOWER(CONCAT('%', :nombre, '%')))
        """,
        countQuery = """
        SELECT COUNT(s) FROM SolicitudFormacion s
        JOIN s.estudiante e JOIN e.usuario u
        JOIN s.formacion f
        WHERE f.centro.id = :centroId
          AND (:formacionId IS NULL OR f.id = :formacionId)
          AND (:#{#estados == null || #estados.isEmpty()} = true OR s.estado IN :estados)
          AND (:nombre = '' OR LOWER(CONCAT(u.nombre, ' ', u.apellidos)) LIKE LOWER(CONCAT('%', :nombre, '%')))
        """)
    Page<SolicitudFormacion> findByCentroIdWithFilters(
        @Param("centroId") Long centroId,
        @Param("formacionId") Long formacionId,
        @Param("estados") List<EstadoSolicitud> estados,
        @Param("nombre") String nombre,
        Pageable pageable);

    @Query("SELECT COUNT(s) FROM SolicitudFormacion s " +
           "WHERE s.formacion.centro.id = :centroId AND s.estado = :estado")
    long countByCentroIdAndEstado(
        @Param("centroId") Long centroId,
        @Param("estado") EstadoSolicitud estado);

    @Query("""
        SELECT COUNT(s) FROM SolicitudFormacion s
        JOIN s.estudiante e JOIN e.usuario u
        JOIN s.formacion f
        WHERE f.centro.id = :centroId
          AND s.estado = :estado
          AND (:formacionId IS NULL OR f.id = :formacionId)
          AND (:nombre = '' OR LOWER(CONCAT(u.nombre, ' ', u.apellidos)) LIKE LOWER(CONCAT('%', :nombre, '%')))
        """)
    long countByCentroIdWithFilters(
        @Param("centroId") Long centroId,
        @Param("estado") EstadoSolicitud estado,
        @Param("formacionId") Long formacionId,
        @Param("nombre") String nombre);

    // Validar propiedad de solicitud para el gestor
    @Query("SELECT s FROM SolicitudFormacion s " +
           "JOIN FETCH s.estudiante e JOIN FETCH e.usuario " +
           "JOIN FETCH s.formacion f " +
           "WHERE s.id = :id AND f.centro.id = :centroId")
    Optional<SolicitudFormacion> findByIdAndCentroId(
        @Param("id") Long id,
        @Param("centroId") Long centroId);

    // Últimas N solicitudes del estudiante con nombre de formación y estado (para contexto IA)
    @Query("""
        SELECT s.formacion.nombre, s.estado
        FROM SolicitudFormacion s
        WHERE s.estudiante.id = :estId
        ORDER BY s.fechaSolicitud DESC
        """)
    List<Object[]> findTop3ByEstudianteIdOrderByFecha(@Param("estId") Long estId, Pageable pageable);
}
