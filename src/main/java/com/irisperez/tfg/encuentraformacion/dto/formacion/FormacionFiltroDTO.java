package com.irisperez.tfg.encuentraformacion.dto.formacion;

import com.irisperez.tfg.encuentraformacion.model.enums.HorarioFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class FormacionFiltroDTO {
    private String nombre;
    private List<String> nombres; // búsqueda multi-término con OR
    private String tituloOficial;
    private ModalidadFormacion modalidad;
    private Long comunidadAutonomaId;
    private Long provinciaId;
    private String localidad;
    private HorarioFormacion horario;
    private Long tipoEstudiosId;
    private TipoCentro tipoCentro;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Boolean soloGratuitas;
    private LocalDate fechaInicioDesde;
}
