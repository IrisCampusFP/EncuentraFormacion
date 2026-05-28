package com.irisperez.tfg.encuentraformacion.dto.faq;

import lombok.Data;

@Data
public class FaqGestorDTO {
    private Long id;
    private String pregunta;
    private String respuesta;
    private Boolean activa;
    private Integer orden;
}
