package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "formaciones_favoritas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormacionFavorita {

    @EmbeddedId
    private FormacionFavoritaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("estudianteId")
    @JoinColumn(name = "estudiante_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("formacionId")
    @JoinColumn(name = "formacion_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Formacion formacion;

    @Column(name = "fecha_guardado", nullable = false, updatable = false)
    private LocalDateTime fechaGuardado;

    public FormacionFavorita(Estudiante estudiante, Formacion formacion) {
        this.id = new FormacionFavoritaId(estudiante.getId(), formacion.getId());
        this.estudiante = estudiante;
        this.formacion = formacion;
    }

    @PrePersist
    void prePersist() {
        if (fechaGuardado == null) fechaGuardado = LocalDateTime.now();
    }
}
