package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

@Entity
@Table(
    name = "conversaciones",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_conversaciones_estudiante_centro",
        columnNames = {"estudiante_id", "centro_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_conv_estudiante"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estudiante estudiante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_conv_centro"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Centro centro;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad;

    @OneToMany(mappedBy = "conversacion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Mensaje> mensajes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conversacion_formaciones",
        schema = "encuentra_formacion",
        joinColumns = @JoinColumn(name = "conversacion_id"),
        inverseJoinColumns = @JoinColumn(name = "formacion_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Formacion> formaciones = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
        if (ultimaActividad == null) ultimaActividad = LocalDateTime.now();
    }
}
