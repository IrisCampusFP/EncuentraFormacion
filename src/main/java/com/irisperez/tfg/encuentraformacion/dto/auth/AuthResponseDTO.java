package com.irisperez.tfg.encuentraformacion.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO para devolver el token JWT y la información básica del usuario tras un login exitoso.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String email;
    private List<String> roles;
}
