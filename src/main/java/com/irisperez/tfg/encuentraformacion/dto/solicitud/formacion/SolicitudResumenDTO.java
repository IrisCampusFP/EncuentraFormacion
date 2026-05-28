package com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class SolicitudResumenDTO {
    private Long id;
    private Long formacionId;
    private UUID formacionUuid;
    private String formacionNombre;
    private Long centroId;
    private UUID centroUuid;
    private String centroNombre;
    private EstadoSolicitud estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
}
