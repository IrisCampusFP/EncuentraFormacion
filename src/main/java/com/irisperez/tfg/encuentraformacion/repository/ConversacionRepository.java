package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.repository.projection.ConversacionResumenProyeccion;
import com.irisperez.tfg.encuentraformacion.model.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    @Query("SELECT COUNT(c) FROM Conversacion c JOIN c.formaciones f WHERE f.id = :formacionId")
    long countByFormacionId(@Param("formacionId") Long formacionId);

    Optional<Conversacion> findByEstudianteIdAndCentroId(Long estId, Long centroId);

    @Query("""
           SELECT c.id                                   AS id,
                  ct.id                                  AS centroId,
                  ct.uuid                                AS centroUuid,
                  ct.nombreComercial                     AS centroNombre,
                  e.id                                   AS estudianteId,
                  u.nombre                               AS estudianteNombre,
                  c.ultimaActividad                      AS ultimaActividad,
                  (SELECT COUNT(m) FROM Mensaje m
                   WHERE m.conversacion.id = c.id
                     AND m.remitente.id <> :usuarioId
                     AND m.leido = false)                AS noLeidos,
                  (SELECT m2.contenido FROM Mensaje m2
                   WHERE m2.conversacion.id = c.id
                   ORDER BY m2.fechaEnvio DESC
                   LIMIT 1)                              AS ultimoMensaje,
                  (SELECT CASE WHEN m3.remitente.id = :usuarioId THEN true ELSE false END
                   FROM Mensaje m3
                   WHERE m3.conversacion.id = c.id
                   ORDER BY m3.fechaEnvio DESC
                   LIMIT 1)                              AS ultimoMensajeEsMio
           FROM Conversacion c
           JOIN c.centro ct
           JOIN c.estudiante e
           JOIN e.usuario u
           WHERE e.id = :estId
           ORDER BY c.ultimaActividad DESC
           """)
    List<ConversacionResumenProyeccion> findResumenByEstudianteId(
            @Param("estId") Long estId,
            @Param("usuarioId") Long usuarioId);

    @Query("SELECT DISTINCT c FROM Conversacion c " +
           "JOIN FETCH c.estudiante e " +
           "JOIN FETCH e.usuario " +
           "LEFT JOIN FETCH c.formaciones " +
           "WHERE c.centro.id = :centroId " +
           "ORDER BY c.ultimaActividad DESC")
    List<Conversacion> findByCentroIdOrdenadas(@Param("centroId") Long centroId);

    @Query("""
           SELECT DISTINCT c FROM Conversacion c
           JOIN FETCH c.estudiante e
           JOIN FETCH e.usuario
           LEFT JOIN FETCH c.formaciones
           WHERE c.centro.id = :centroId 
             AND EXISTS (SELECT 1 FROM c.formaciones f WHERE f.id = :formacionId)
           ORDER BY c.ultimaActividad DESC
           """)
    List<Conversacion> findByCentroIdAndFormacionIdOrdenadas(
        @Param("centroId") Long centroId,
        @Param("formacionId") Long formacionId);

    @Query("SELECT c FROM Conversacion c LEFT JOIN FETCH c.formaciones WHERE c.id = :id")
    Optional<Conversacion> findByIdWithFormaciones(@Param("id") Long id);

    @Query("SELECT c FROM Conversacion c LEFT JOIN FETCH c.formaciones WHERE c.id IN :ids")
    List<Conversacion> findByIdsWithFormaciones(@Param("ids") List<Long> ids);
}
