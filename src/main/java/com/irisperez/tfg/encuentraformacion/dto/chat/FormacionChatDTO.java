package com.irisperez.tfg.encuentraformacion.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FormacionChatDTO {
    private Long id;
    private UUID uuid;
    private String nombre;
    private String tipoEstudios;
}
