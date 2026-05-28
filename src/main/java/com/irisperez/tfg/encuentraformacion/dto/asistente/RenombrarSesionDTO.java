package com.irisperez.tfg.encuentraformacion.dto.asistente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenombrarSesionDTO {

    @NotBlank
    @Size(max = 100)
    private String titulo;
}
