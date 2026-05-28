package com.irisperez.tfg.encuentraformacion.dto.auth;

import lombok.Data;

@Data
public class RegistroGestorRequestDTO {
    
    // Objeto con los datos del usuario a registrar
    private RegistroRequestDTO datosUsuario;
    
    // ID del centro si el usuario ha seleccionado uno ya existente
    private Long idCentroExistente;
    
    // Objeto con los datos del nuevo centro a registrar (se registrará un nuevo centro si idCentroExistente es null)
    private RegistroCentroRequestDTO datosCentroNuevo;

}
