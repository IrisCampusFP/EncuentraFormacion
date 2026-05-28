package com.irisperez.tfg.encuentraformacion.dto.centro;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import lombok.Data;
import java.util.UUID;

@Data
public class CentroGestorDTO {
    private Long id;
    private UUID uuid;
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
    private Boolean verificado;
    private Boolean tieneGestor;
}
