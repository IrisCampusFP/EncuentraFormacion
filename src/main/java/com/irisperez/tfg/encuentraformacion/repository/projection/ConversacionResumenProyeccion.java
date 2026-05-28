package com.irisperez.tfg.encuentraformacion.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ConversacionResumenProyeccion {
    Long getId();
    Long getCentroId();
    UUID getCentroUuid();
    String getCentroNombre();
    Long getEstudianteId();
    String getEstudianteNombre();
    LocalDateTime getUltimaActividad();
    Long getNoLeidos();
    String getUltimoMensaje();
    Boolean getUltimoMensajeEsMio();
}
