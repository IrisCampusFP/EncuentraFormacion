package com.irisperez.tfg.encuentraformacion.dto.asistente;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SesionIAResumenDTO {
    private Long id;
    private String titulo;
    private LocalDateTime fechaInicio;
    private LocalDateTime ultimaActividad;
    private long totalMensajes;
}
