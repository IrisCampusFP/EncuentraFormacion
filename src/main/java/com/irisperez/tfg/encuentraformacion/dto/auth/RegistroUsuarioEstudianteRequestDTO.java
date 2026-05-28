package com.irisperez.tfg.encuentraformacion.dto.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistroUsuarioEstudianteRequestDTO {
    @Valid // @Valid activa la validación de los campos del DTO
    @NotNull(message = "Los datos del usuario son obligatorios")
    private RegistroRequestDTO datosUsuario;

    private String gradoEstudios;

    private Long provinciaId;

    private String localidad;
}
