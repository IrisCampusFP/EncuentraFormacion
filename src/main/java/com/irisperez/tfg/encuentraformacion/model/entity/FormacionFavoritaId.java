package com.irisperez.tfg.encuentraformacion.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormacionFavoritaId implements Serializable {

    @Column(name = "estudiante_id")
    private Long estudianteId;

    @Column(name = "formacion_id")
    private Long formacionId;
}
