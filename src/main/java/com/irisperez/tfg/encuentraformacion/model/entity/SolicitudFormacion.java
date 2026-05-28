package com.irisperez.tfg.encuentraformacion.model.entity;

import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "solicitudes_formacion",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_solicitudes_estudiante_formacion",
        columnNames = {"estudiante_id", "formacion_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudFormacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "estudiante_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_solicitudes_estudiante")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estudiante estudiante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "formacion_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_solicitudes_formacion")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Formacion formacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoSolicitud estado;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @PrePersist
    void prePersist() {
        if (fechaSolicitud == null) fechaSolicitud = LocalDateTime.now();
        if (estado == null) estado = EstadoSolicitud.PENDIENTE;
    }
}