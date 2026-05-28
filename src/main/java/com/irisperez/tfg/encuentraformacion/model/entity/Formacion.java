package com.irisperez.tfg.encuentraformacion.model.entity;

import com.irisperez.tfg.encuentraformacion.model.enums.HorarioFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "formaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Formacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_formacion")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "centro_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_formaciones_centro")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Centro centro;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "titulo_oficial")
    private String tituloOficial;

    @Column(name = "descripcion")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tipo_estudios",
            foreignKey = @ForeignKey(name = "fk_formaciones_tipo_estudios")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TipoEstudios tipoEstudios;

    @Enumerated(EnumType.STRING)
    @Column(name = "horario")
    private HorarioFormacion horario;

    @Column(name = "duracion_horas")
    private Integer duracionHoras;

    @Column(name = "activa")
    private Boolean activa;

    @Column(name = "precio", precision = 10, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false)
    private ModalidadFormacion modalidad;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;


    @OneToMany(mappedBy = "formacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SolicitudFormacion> solicitudes = new HashSet<>();

    @OneToMany(mappedBy = "formacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Valoracion> valoraciones = new HashSet<>();

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (fechaAlta == null) fechaAlta = LocalDateTime.now();
        if (activa == null) activa = Boolean.TRUE;
    }

    @PreUpdate
    void preUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}