package com.irisperez.tfg.encuentraformacion.model.entity;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoEventoSolicitud;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_solicitud_chat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoSolicitudChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conversacion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_evento_conversacion"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Conversacion conversacion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_evento_solicitud"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SolicitudFormacion solicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoSolicitud tipoEvento;

    @Column(name = "formacion_nombre", nullable = false)
    private String formacionNombre;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
