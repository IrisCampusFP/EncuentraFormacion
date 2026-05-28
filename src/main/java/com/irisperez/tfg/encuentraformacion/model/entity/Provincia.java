package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "m_provincias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provincia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_provincia")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "codigo_ine", nullable = false, unique = true, length = 2)
    private String codigoIne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comunidad_autonoma_id", nullable = false)
    private ComunidadAutonoma comunidadAutonoma;
}
