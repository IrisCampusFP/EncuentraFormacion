package com.irisperez.tfg.encuentraformacion.model.entity;

import com.irisperez.tfg.encuentraformacion.model.enums.RolMensajeIA;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_ia", schema = "encuentra_formacion")
@Getter
@NoArgsConstructor
public class MensajeIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SesionIA sesion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RolMensajeIA rol;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    void prePersist() {
        if (fechaEnvio == null) fechaEnvio = LocalDateTime.now();
    }

    public MensajeIA(SesionIA sesion, RolMensajeIA rol, String contenido) {
        this.sesion = sesion;
        this.rol = rol;
        this.contenido = contenido;
    }
}
