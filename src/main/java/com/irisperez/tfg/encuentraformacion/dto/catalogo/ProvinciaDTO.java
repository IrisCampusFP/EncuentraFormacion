package com.irisperez.tfg.encuentraformacion.dto.catalogo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProvinciaDTO {
    private Long id;
    private String nombre;
    private String codigoIne;
    private Long comunidadAutonomaId;
    private String comunidadAutonoma;
}
