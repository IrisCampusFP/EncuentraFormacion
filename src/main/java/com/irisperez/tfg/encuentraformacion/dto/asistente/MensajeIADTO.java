package com.irisperez.tfg.encuentraformacion.dto.asistente;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MensajeIADTO {
    private Long id;
    private String rol;       // "USER" o "ASSISTANT"
    private String contenido;
    private LocalDateTime fechaEnvio;
}
