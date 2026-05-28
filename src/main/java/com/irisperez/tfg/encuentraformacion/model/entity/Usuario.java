package com.irisperez.tfg.encuentraformacion.model.entity;

import com.irisperez.tfg.encuentraformacion.model.enums.Sexo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "email", nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "dni", length = 9, unique = true)
    private String dni;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo")
    private Sexo sexo;

    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "activo")
    private Boolean activo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rol_usuario",
        joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "rol_id", referencedColumnName = "id_rol")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Rol> roles = new ArrayList<>();

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estudiante estudiante;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SolicitudGestion> solicitudesGestion = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "gestores_centros",
        joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "centro_id", referencedColumnName = "id_centro")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Centro> centrosGestionados = new HashSet<>();


    // Atributo que no existe en la BD, solo para controlar la logica
    // de número de intentos fallidos en el inicio de sesión
    @Transient
    private Integer intentosFallidos;

    @PrePersist
    void prePersist() {
        if (fechaAlta == null) fechaAlta = LocalDateTime.now();
        if (activo == null) activo = Boolean.TRUE;
    }

    @PreUpdate
    void preUpdate() {
        fechaModificacion = LocalDateTime.now();
    }

}