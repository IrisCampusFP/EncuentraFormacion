package com.irisperez.tfg.encuentraformacion.dto.auth;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroCentroRequestDTO {

    private String nombreComercial;
    private String codigo;
    private String direccion;
    private String localidad;
    private String provincia;
    private String telefono;
    private String email;
    private String paginaWeb;
    private TipoCentro tipo;

}
