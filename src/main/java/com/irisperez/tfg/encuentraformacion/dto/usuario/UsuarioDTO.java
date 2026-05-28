package com.irisperez.tfg.encuentraformacion.dto.usuario;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.RolDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.Sexo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String email;
    private String nombre;
    private String apellidos;
    private String username;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String dni;
    private Sexo sexo;
    private List<RolDTO> roles = new ArrayList<>();
    private LocalDateTime ultimaConexion;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaAlta;
    private Boolean activo;
    private Boolean tieneSolicitudGestionPendiente;
    private Long gradoEstudiosId;
    private String gradoEstudiosNombre;
    private Long provinciaId;
    private String provinciaNombre;
    private String localidad;
    private Set<CentroResumenDTO> centrosGestionados = new HashSet<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CentroResumenDTO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String nombreComercial;
    }
}
