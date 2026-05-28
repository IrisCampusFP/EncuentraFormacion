package com.irisperez.tfg.encuentraformacion.dto.formacion;

import com.irisperez.tfg.encuentraformacion.model.enums.HorarioFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FormacionGestorDTO {
    private Long id;
    private String nombre;
    private String tituloOficial;
    private String descripcion;
    private String tipoEstudiosNombre;
    private Long tipoEstudiosId;
    private HorarioFormacion horario;
    private ModalidadFormacion modalidad;
    private Integer duracionHoras;
    private BigDecimal precio;
    private Boolean activa;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDateTime fechaAlta;
    private long solicitudesPendientes;
    private long chatsActivos;
}
