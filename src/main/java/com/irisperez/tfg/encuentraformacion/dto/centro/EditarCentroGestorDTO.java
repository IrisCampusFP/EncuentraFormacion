package com.irisperez.tfg.encuentraformacion.dto.centro;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditarCentroGestorDTO {
    private String descripcion;

    @Size(max = 255)
    private String direccion;

    @Size(max = 100)
    private String localidad;

    private TipoCentro tipo;

    @Size(max = 20)
    private String telefono;

    @Email @Size(max = 150)
    private String email;

    private String paginaWeb;
}
