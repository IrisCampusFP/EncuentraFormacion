package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudGestion;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudGestionRepository extends JpaRepository<SolicitudGestion, Long> {

    // Encontrar solicitudes por estado
    List<SolicitudGestion> findByEstado(EstadoSolicitud estado);

    // Versión paginada por estado (para el panel de pendientes)
    Page<SolicitudGestion> findByEstado(EstadoSolicitud estado, Pageable pageable);

    // Obtener la solicitud más reciente de un usuario (para compatibilidad con estado-solicitud.html sin ?id)
    SolicitudGestion findTopByUsuarioOrderByFechaSolicitudDesc(Usuario usuario);

    // Obtener solicitud por id y usuario (para validar propiedad)
    java.util.Optional<SolicitudGestion> findByIdAndUsuario(Long id, Usuario usuario);

    // Listar todas las solicitudes de un usuario ordenadas por fecha descendente
    List<SolicitudGestion> findAllByUsuarioOrderByFechaSolicitudDesc(Usuario usuario);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoSolicitud estado);

    boolean existsByUsuarioIdAndCentroIdAndEstado(Long usuarioId, Long centroId, EstadoSolicitud estado);

    // Historial (lista completa — uso interno)
    @Query("SELECT s FROM SolicitudGestion s WHERE s.estado <> :estado ORDER BY s.fechaSolicitud DESC")
    List<SolicitudGestion> obtenerHistorialSolicitudes(@Param("estado") EstadoSolicitud estado);

    // Historial paginado para el panel de administración
    @Query(value = "SELECT s FROM SolicitudGestion s WHERE s.estado <> :estado",
           countQuery = "SELECT COUNT(s) FROM SolicitudGestion s WHERE s.estado <> :estado")
    Page<SolicitudGestion> obtenerHistorialSolicitudesPaginado(@Param("estado") EstadoSolicitud estado, Pageable pageable);

    // Solicitudes pendientes con búsqueda de texto y filtro de verificación del centro
    @Query(value = """
            SELECT s FROM SolicitudGestion s
            WHERE s.estado = :estadoPendiente
              AND (:q = '' OR LOWER(s.usuario.nombre)         LIKE CONCAT('%', :q, '%')
                           OR (s.usuario.apellidos IS NOT NULL
                               AND LOWER(s.usuario.apellidos) LIKE CONCAT('%', :q, '%'))
                           OR LOWER(s.centro.nombreComercial) LIKE CONCAT('%', :q, '%'))
              AND (:verificadoCentro IS NULL OR s.centro.verificado = :verificadoCentro)
            """,
           countQuery = """
            SELECT COUNT(s) FROM SolicitudGestion s
            WHERE s.estado = :estadoPendiente
              AND (:q = '' OR LOWER(s.usuario.nombre)         LIKE CONCAT('%', :q, '%')
                           OR (s.usuario.apellidos IS NOT NULL
                               AND LOWER(s.usuario.apellidos) LIKE CONCAT('%', :q, '%'))
                           OR LOWER(s.centro.nombreComercial) LIKE CONCAT('%', :q, '%'))
              AND (:verificadoCentro IS NULL OR s.centro.verificado = :verificadoCentro)
            """)
    Page<SolicitudGestion> buscarPendientesConFiltro(
            @Param("estadoPendiente") EstadoSolicitud estadoPendiente,
            @Param("q") String q,
            @Param("verificadoCentro") Boolean verificadoCentro,
            Pageable pageable);

    // Historial con búsqueda de texto y filtro de estado opcional
    @Query(value = """
            SELECT s FROM SolicitudGestion s
            WHERE s.estado <> :estadoExcluir
              AND (:estadoFiltro IS NULL OR s.estado = :estadoFiltro)
              AND (:q = '' OR LOWER(s.usuario.nombre)         LIKE CONCAT('%', :q, '%')
                           OR (s.usuario.apellidos IS NOT NULL
                               AND LOWER(s.usuario.apellidos) LIKE CONCAT('%', :q, '%'))
                           OR LOWER(s.centro.nombreComercial) LIKE CONCAT('%', :q, '%'))
            """,
           countQuery = """
            SELECT COUNT(s) FROM SolicitudGestion s
            WHERE s.estado <> :estadoExcluir
              AND (:estadoFiltro IS NULL OR s.estado = :estadoFiltro)
              AND (:q = '' OR LOWER(s.usuario.nombre)         LIKE CONCAT('%', :q, '%')
                           OR (s.usuario.apellidos IS NOT NULL
                               AND LOWER(s.usuario.apellidos) LIKE CONCAT('%', :q, '%'))
                           OR LOWER(s.centro.nombreComercial) LIKE CONCAT('%', :q, '%'))
            """)
    Page<SolicitudGestion> buscarHistorialConFiltros(
            @Param("estadoExcluir") EstadoSolicitud estadoExcluir,
            @Param("estadoFiltro") EstadoSolicitud estadoFiltro,
            @Param("q") String q,
            Pageable pageable);
}
