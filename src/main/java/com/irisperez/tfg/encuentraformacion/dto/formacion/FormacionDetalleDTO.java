package com.irisperez.tfg.encuentraformacion.dto.formacion;

import com.irisperez.tfg.encuentraformacion.model.enums.HorarioFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.irisperez.tfg.encuentraformacion.dto.catalogo.TipoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.ValoracionDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;

@Data
@NoArgsConstructor
public class FormacionDetalleDTO {
    @JsonIgnore
    private Long id;
    private UUID uuid;
    private String nombre;
    private String descripcion;
    private String tituloOficial;
    private ModalidadFormacion modalidad;
    private HorarioFormacion horario;
    private BigDecimal precio;
    private Integer duracionHoras;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private TipoEstudiosDTO tipoEstudios;

    @JsonIgnore
    private Long centroId;
    private UUID centroUuid;
    private String centroNombre;
    private String centroLocalidad;
    private String centroProvincia;
    private Boolean centroVerificado;
    private String centroDescripcion;
    private String centroDireccion;
    private String centroTelefono;
    private String centroEmail;
    private String centroPaginaWeb;

    private TipoCentro centroTipo;
    private Boolean centroTieneGestor;

    private List<ValoracionDTO> valoraciones;
    private Double valoracionMedia;
    private Long totalValoraciones;
}
