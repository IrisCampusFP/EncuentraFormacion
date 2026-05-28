package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Mensaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @Query("SELECT m FROM Mensaje m " +
           "JOIN FETCH m.remitente " +
           "WHERE m.conversacion.id = :convId " +
           "ORDER BY m.fechaEnvio ASC")
    Page<Mensaje> findByConversacionIdOrdenados(@Param("convId") Long convId, Pageable pageable);

    @Modifying
    @Query("UPDATE Mensaje m SET m.leido = true " +
           "WHERE m.conversacion.id = :convId AND m.remitente.id <> :usuarioId AND m.leido = false")
    void marcarLeidosPorDestinatario(@Param("convId") Long convId, @Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(m) FROM Mensaje m " +
           "WHERE m.conversacion.id = :convId AND m.remitente.id <> :usuarioId AND m.leido = false")
    Long countNoLeidosPorDestinatario(@Param("convId") Long convId, @Param("usuarioId") Long usuarioId);

    // Todos los mensajes de una conversación sin paginar (para gestor)
    @Query("SELECT m FROM Mensaje m JOIN FETCH m.remitente " +
           "WHERE m.conversacion.id = :convId ORDER BY m.fechaEnvio ASC")
    java.util.List<Mensaje> findAllByConversacionId(@Param("convId") Long convId);

    // Último mensaje de una conversación (para preview en lista)
    @Query("SELECT m FROM Mensaje m WHERE m.conversacion.id = :convId ORDER BY m.fechaEnvio DESC LIMIT 1")
    java.util.Optional<Mensaje> findUltimoMensaje(@Param("convId") Long convId);

    @Query("""
           SELECT COUNT(DISTINCT m.id) FROM Mensaje m
           JOIN m.conversacion c
           LEFT JOIN c.centro.gestores g
           WHERE m.remitente.id <> :usuarioId
             AND m.leido = false
             AND (c.estudiante.usuario.id = :usuarioId OR g.id = :usuarioId)
           """)
    Long countTotalNoLeidosPorUsuario(@Param("usuarioId") Long usuarioId);
}
