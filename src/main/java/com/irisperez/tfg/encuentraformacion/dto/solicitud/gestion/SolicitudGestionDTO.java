package com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion;

import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudGestionDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long idUsuario;
    private String nombre;
    private String apellidos;
    private Long idCentro;
    private String nombreCentro;
    private Boolean verificadoCentro;
    private EstadoSolicitud estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
}
