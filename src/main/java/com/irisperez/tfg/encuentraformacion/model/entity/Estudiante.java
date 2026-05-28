package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "estudiantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante")
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "usuario_id",
        nullable = false,
        unique = true,
        foreignKey = @ForeignKey(name = "fk_estudiantes_usuario")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "grado_estudios_id",
        foreignKey = @ForeignKey(name = "fk_estudiantes_grado_estudios")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private GradoEstudios gradoEstudios;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "provincia_id",
        foreignKey = @ForeignKey(name = "fk_estudiantes_provincia")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Provincia provincia;

    @Column(name = "localidad", length = 100)
    private String localidad;

    @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SolicitudFormacion> solicitudes = new HashSet<>();

    @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Valoracion> valoraciones = new HashSet<>();

}