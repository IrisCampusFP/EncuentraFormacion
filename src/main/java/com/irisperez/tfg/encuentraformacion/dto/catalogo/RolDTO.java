package com.irisperez.tfg.encuentraformacion.dto.catalogo;

import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private RolNombre nombre;
}