package com.irisperez.tfg.encuentraformacion.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class ConversacionMensajesDTO {
    private Long id;
    private List<FormacionChatDTO> formaciones;
    private List<MensajeChatDTO> mensajes;
}
