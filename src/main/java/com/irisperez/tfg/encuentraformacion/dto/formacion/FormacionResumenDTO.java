package com.irisperez.tfg.encuentraformacion.dto.formacion;

import com.irisperez.tfg.encuentraformacion.model.enums.HorarioFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.irisperez.tfg.encuentraformacion.dto.catalogo.TipoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;

@Data
@NoArgsConstructor
public class FormacionResumenDTO {
    @JsonIgnore
    private Long id;
    private UUID uuid;
    private String nombre;
    private String tituloOficial;
    private String descripcion;
    private ModalidadFormacion modalidad;
    private HorarioFormacion horario;
    private BigDecimal precio;
    private Integer duracionHoras;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    @JsonIgnore
    private Long centroId;
    private UUID centroUuid;
    private String centroNombre;
    private String centroLocalidad;
    private TipoCentro centroTipo;
    private TipoEstudiosDTO tipoEstudios;
    private Double valoracionMedia;
    private Long totalValoraciones;
}
