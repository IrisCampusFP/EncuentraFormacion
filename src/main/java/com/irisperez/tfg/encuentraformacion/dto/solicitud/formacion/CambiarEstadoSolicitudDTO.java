package com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoSolicitudDTO {
    @NotNull
    private EstadoSolicitud estado;
}
