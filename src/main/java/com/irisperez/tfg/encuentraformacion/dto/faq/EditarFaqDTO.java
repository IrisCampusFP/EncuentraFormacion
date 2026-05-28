package com.irisperez.tfg.encuentraformacion.dto.faq;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditarFaqDTO {
    @Size(max = 500)
    private String pregunta;
    private String respuesta;
    private Integer orden;
}
