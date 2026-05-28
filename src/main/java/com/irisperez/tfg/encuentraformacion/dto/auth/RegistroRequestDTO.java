package com.irisperez.tfg.encuentraformacion.dto.auth;

import com.irisperez.tfg.encuentraformacion.model.enums.Sexo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RegistroRequestDTO {

    /*
     * Anotaciones utilizadas:
     * - @NotBlank: Indica que el campo no puede estar vacío ni ser solo espacios en blanco.
     * - @Email: Indica que el campo debe tener un formato de correo electrónico válido.
     * - @Size: Indica que el campo debe tener un tamaño mínimo y/o máximo.
     * - @Pattern: Indica que el campo debe cumplir con una expresión regular.
     *     - regexp: Expresión regular que debe cumplir el campo.
     *     (en este caso ".*\\d.*" indica que la contraseña debe contener al menos un número)
     */

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>\\-_=+\\[\\]]).{8,}$",
        message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un carácter especial (!@#$%^&*...)"
    )
    private String password;

    private LocalDate fechaNacimiento;
    private String telefono;
    private String dni;
    private Sexo sexo;
}
