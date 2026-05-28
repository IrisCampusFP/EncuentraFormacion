package com.irisperez.tfg.encuentraformacion.dto.centro;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CentroDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long id;
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
    private LocalDateTime fechaVerificacion;
    private Boolean tieneGestor;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaModificacion;
}
