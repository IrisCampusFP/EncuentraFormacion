package com.irisperez.tfg.encuentraformacion.dto.notificacion;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificacionDTO {
    private Long id;
    private TipoNotificacion tipo;
    private String titulo;
    private String mensaje;
    private String urlReferencia;
    private Boolean leida;
    private LocalDateTime fechaCreacion;
}
