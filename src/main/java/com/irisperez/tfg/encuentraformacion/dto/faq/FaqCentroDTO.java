package com.irisperez.tfg.encuentraformacion.dto.faq;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FaqCentroDTO {
    private Long id;
    private String pregunta;
    private String respuesta;
    private Integer orden;
}
