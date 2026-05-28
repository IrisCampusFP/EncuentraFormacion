package com.irisperez.tfg.encuentraformacion.dto.centro;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CentroUpdateDTO {
    
    private String nombreComercial;
    private String codigo;
    private String descripcion;
    private String direccion;
    private String localidad;
    private String provincia;
    private TipoCentro tipo;
    private String telefono;
    private String email;
    private String paginaWeb;
}
