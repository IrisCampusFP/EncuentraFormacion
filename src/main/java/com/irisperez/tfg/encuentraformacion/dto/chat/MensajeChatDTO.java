package com.irisperez.tfg.encuentraformacion.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MensajeChatDTO {
    private Long id;
    private Long remitenteId;
    private String remitenteNombre;
    private String contenido;
    private LocalDateTime fechaEnvio;
    private Boolean leido;
}
