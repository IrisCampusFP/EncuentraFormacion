package com.irisperez.tfg.encuentraformacion.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EnviarMensajeDTO {

    @NotBlank
    @Size(max = 2000)
    private String contenido;
}
