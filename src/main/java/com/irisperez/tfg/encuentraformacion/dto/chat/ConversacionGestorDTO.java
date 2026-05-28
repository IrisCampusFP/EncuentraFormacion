package com.irisperez.tfg.encuentraformacion.dto.chat;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversacionGestorDTO {
    private Long id;
    private Long estudianteId;
    private String estudianteNombre;
    private String ultimoMensaje;
    private LocalDateTime ultimaFecha;
    private long mensajesNoLeidos;
    private Boolean ultimoMensajeEsMio;
    private java.util.List<FormacionResumenDTO> formaciones;
}
