package com.irisperez.tfg.encuentraformacion.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class EventoSolicitudChatDTO {
    private Long id;
    private Long solicitudId;
    private String tipoEvento;
    private String formacionNombre;
    private UUID formacionUuid;
    private LocalDateTime fecha;
}
