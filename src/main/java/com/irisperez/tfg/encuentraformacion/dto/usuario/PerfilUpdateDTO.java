package com.irisperez.tfg.encuentraformacion.dto.usuario;

import com.irisperez.tfg.encuentraformacion.model.enums.Sexo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class PerfilUpdateDTO {

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @jakarta.validation.constraints.Email
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 150)
    private String apellidos;

    @Size(max = 20)
    private String telefono;

    private LocalDate fechaNacimiento;

    private Sexo sexo;

    private Long gradoEstudiosId;

    private Long provinciaId;

    @Size(max = 100)
    private String localidad;
}
