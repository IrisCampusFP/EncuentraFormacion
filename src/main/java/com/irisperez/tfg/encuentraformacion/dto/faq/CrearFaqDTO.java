package com.irisperez.tfg.encuentraformacion.dto.faq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearFaqDTO {
    @NotBlank @Size(max = 500)
    private String pregunta;

    @NotBlank
    private String respuesta;
}
