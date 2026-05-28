package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.EventoSolicitudChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventoSolicitudChatRepository extends JpaRepository<EventoSolicitudChat, Long> {

    @Query("SELECT e FROM EventoSolicitudChat e WHERE e.conversacion.id = :convId ORDER BY e.fecha ASC")
    List<EventoSolicitudChat> findByConversacionId(@Param("convId") Long convId);
}
