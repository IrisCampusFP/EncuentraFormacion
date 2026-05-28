package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "m_comunidades_autonomas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComunidadAutonoma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comunidad_autonoma")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "codigo_ine", nullable = false, unique = true, length = 2)
    private String codigoIne;
}
