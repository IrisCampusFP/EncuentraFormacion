package com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion;

import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SolicitudGestorDTO {
    private Long id;
    private String estudianteNombre;
    private String estudianteApellidos;
    private String estudianteEmail;
    private String formacionNombre;
    private EstadoSolicitud estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
}
