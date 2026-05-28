package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_ia", schema = "encuentra_formacion")
@Getter
@NoArgsConstructor
public class SesionIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estudiante estudiante;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad;

    @PrePersist
    void prePersist() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
        if (ultimaActividad == null) ultimaActividad = LocalDateTime.now();
    }

    public SesionIA(Estudiante estudiante, String titulo) {
        this.estudiante = estudiante;
        this.titulo = titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setUltimaActividad(LocalDateTime t) {
        this.ultimaActividad = t;
    }
}
