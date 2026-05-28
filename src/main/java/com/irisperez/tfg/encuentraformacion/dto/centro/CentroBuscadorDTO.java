package com.irisperez.tfg.encuentraformacion.dto.centro;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class CentroBuscadorDTO {
    private UUID uuid;
    private String nombreComercial;
    private String descripcion;
    private String direccion;
    private String localidad;
    private String provincia;
    private TipoCentro tipo;
    private String telefono;
    private String email;
    private String paginaWeb;
    private Double valoracionMedia;
    private Integer totalValoraciones;
    private Integer totalFormaciones;
}
