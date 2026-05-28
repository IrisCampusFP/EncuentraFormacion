package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "valoraciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Valoracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valoracion")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "estudiante_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_valoraciones_estudiante")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estudiante estudiante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "formacion_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_valoraciones_formacion")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Formacion formacion;

    @Column(name = "estrellas", nullable = false)
    private Integer estrellas;

    @Column(name = "comentario")
    private String comentario;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}