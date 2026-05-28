package com.irisperez.tfg.encuentraformacion.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class IniciarChatDTO {

    @NotNull(message = "El UUID del centro es obligatorio")
    private UUID centroUuid;

    @NotNull(message = "El UUID de la formación es obligatorio")
    private UUID formacionUuid;
}
