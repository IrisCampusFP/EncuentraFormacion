package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conversacion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_mensaje_conversacion"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Conversacion conversacion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "remitente_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_mensaje_remitente"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario remitente;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "leido", nullable = false)
    private Boolean leido;

    @PrePersist
    void prePersist() {
        if (fechaEnvio == null) fechaEnvio = LocalDateTime.now();
        if (leido == null) leido = Boolean.FALSE;
    }
}
