package com.irisperez.tfg.encuentraformacion.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ConversacionResumenDTO {
    private Long id;
    private Long centroId;
    private UUID centroUuid;
    private String centroNombre;
    private Long estudianteId;
    private String estudianteNombre;
    private String ultimoMensaje;
    private LocalDateTime ultimaActividad;
    private Long noLeidos;
    private Boolean ultimoMensajeEsMio;
    private List<FormacionChatDTO> formaciones;
}
