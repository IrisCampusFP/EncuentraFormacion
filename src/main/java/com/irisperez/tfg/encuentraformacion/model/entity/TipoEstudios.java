package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "m_tipo_estudios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoEstudios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_estudios")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @OneToMany(mappedBy = "tipoEstudios", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Formacion> formaciones = new HashSet<>();
}
