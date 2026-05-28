package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad que representa un token JWT invalidado para mitigar riesgos de sesión y autenticación.
 * Se almacena el hash SHA-256 del token, nunca el token en texto claro.
 */
@Entity
@Table(name = "tokens_invalidados", schema = "encuentra_formacion")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenInvalidado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_invalidacion", nullable = false)
    private LocalDateTime fechaInvalidacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    public TokenInvalidado(String tokenHash, LocalDateTime fechaInvalidacion, LocalDateTime fechaExpiracion) {
        this.tokenHash = tokenHash;
        this.fechaInvalidacion = fechaInvalidacion;
        this.fechaExpiracion = fechaExpiracion;
    }
}
