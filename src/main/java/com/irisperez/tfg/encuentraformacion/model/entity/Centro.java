package com.irisperez.tfg.encuentraformacion.model.entity;

import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "centros")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Centro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_centro")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "nombre_comercial", nullable = false, length = 200)
    private String nombreComercial;

    @Column(name = "codigo", nullable = false, length = 8, unique = true, columnDefinition = "CHAR(8)")
    private String codigo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "direccion", nullable = false)
    private String direccion;

    @Column(name = "localidad", nullable = false, length = 100)
    private String localidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provincia_id", nullable = false)
    private Provincia provincia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCentro tipo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    @Column(name = "pagina_web")
    private String paginaWeb;

    @Column(name = "verificado")
    private Boolean verificado;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Column(name = "tiene_gestor")
    private Boolean tieneGestor;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @ManyToMany(mappedBy = "centrosGestionados", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Usuario> gestores = new HashSet<>();

    @OneToMany(mappedBy = "centro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Formacion> formaciones = new HashSet<>();

    @OneToMany(mappedBy = "centro", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SolicitudGestion> solicitudesGestion = new HashSet<>();

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (fechaAlta == null) fechaAlta = LocalDateTime.now();
        if (verificado == null) verificado = Boolean.FALSE;
        if (tieneGestor == null) tieneGestor = Boolean.FALSE;
    }

    @PreUpdate
    void preUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}