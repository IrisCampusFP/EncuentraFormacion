package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq_centro")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaqCentro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_faq")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "centro_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_faq_centro")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Centro centro;

    @Column(name = "pregunta", nullable = false, length = 500)
    private String pregunta;

    @Column(name = "respuesta", nullable = false, columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "activa")
    private Boolean activa;

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @PrePersist
    void prePersist() {
        if (fechaAlta == null) fechaAlta = LocalDateTime.now();
        if (orden == null) orden = 0;
        if (activa == null) activa = Boolean.TRUE;
    }
}
