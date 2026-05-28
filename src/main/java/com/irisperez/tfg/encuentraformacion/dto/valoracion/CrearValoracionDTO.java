package com.irisperez.tfg.encuentraformacion.dto.valoracion;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CrearValoracionDTO {

    @NotNull
    @Min(1) @Max(5)
    private Integer estrellas;

    @NotNull
    private java.util.UUID formacionUuid;

    @Size(max = 1000)
    private String comentario;
}
