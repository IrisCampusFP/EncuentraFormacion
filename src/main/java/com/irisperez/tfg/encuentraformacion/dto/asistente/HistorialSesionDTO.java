package com.irisperez.tfg.encuentraformacion.dto.asistente;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HistorialSesionDTO {
    private Long sesionId;
    private String titulo;
    private List<MensajeIADTO> mensajes;
}
