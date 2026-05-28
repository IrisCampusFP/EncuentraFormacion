package com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NuevaSolicitudGestionDTO {

    @NotNull(message = "El identificador del centro es obligatorio.")
    private Long centroId;
}
