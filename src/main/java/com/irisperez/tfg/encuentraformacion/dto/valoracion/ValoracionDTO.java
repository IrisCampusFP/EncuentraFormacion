package com.irisperez.tfg.encuentraformacion.dto.valoracion;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ValoracionDTO {
    private Long id;
    private Integer estrellas;
    private String comentario;
    private LocalDateTime fecha;
    private LocalDateTime fechaModificacion;
    private String nombreEstudiante;
}
