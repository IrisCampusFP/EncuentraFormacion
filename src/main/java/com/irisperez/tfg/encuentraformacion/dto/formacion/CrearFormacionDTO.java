package com.irisperez.tfg.encuentraformacion.dto.formacion;

import com.irisperez.tfg.encuentraformacion.model.enums.HorarioFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CrearFormacionDTO {
    @NotBlank
    @Size(max = 200)
    private String nombre;

    @NotNull
    private Long tipoEstudiosId;

    @NotNull
    private HorarioFormacion horario;

    @NotNull
    private ModalidadFormacion modalidad;

    private String descripcion;
    private String tituloOficial;
    private Integer duracionHoras;

    @DecimalMin("0.0")
    private BigDecimal precio;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
