package com.irisperez.tfg.encuentraformacion.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CambiarPasswordDTO {

    @NotBlank
    private String passwordActual;

    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>\\-_=+\\[\\]]).{8,}$",
        message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial."
    )
    private String passwordNueva;
}
