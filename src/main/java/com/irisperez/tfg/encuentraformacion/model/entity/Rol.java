package com.irisperez.tfg.encuentraformacion.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "m_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private RolNombre nombre;

    @JsonIgnore
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Usuario> usuarios = new HashSet<>();
}