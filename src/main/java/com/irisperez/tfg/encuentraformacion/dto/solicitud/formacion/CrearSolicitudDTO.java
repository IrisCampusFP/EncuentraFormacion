package com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class CrearSolicitudDTO {

    @NotNull(message = "El UUID de la formación es obligatorio")
    private UUID formacionUuid;
}
