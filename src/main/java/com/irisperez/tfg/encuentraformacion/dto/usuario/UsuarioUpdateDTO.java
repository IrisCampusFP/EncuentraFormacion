package com.irisperez.tfg.encuentraformacion.dto.usuario;

import com.irisperez.tfg.encuentraformacion.model.enums.Sexo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String email;
    private String nombre;
    private String apellidos;
    private String username;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String dni;
    private Sexo sexo;
    private Boolean activo;
}
