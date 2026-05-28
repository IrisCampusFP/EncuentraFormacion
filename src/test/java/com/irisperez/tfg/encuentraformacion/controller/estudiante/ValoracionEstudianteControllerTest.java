package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.valoracion.CrearValoracionDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.ValoracionDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValoracionEstudianteController")
class ValoracionEstudianteControllerTest {

    @Mock private ValoracionEstudianteService valoracionEstudianteService;
    @InjectMocks private ValoracionEstudianteController controller;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario(); u.setId(1L);
        Rol rol = new Rol(); rol.setNombre(RolNombre.ESTUDIANTE);
        u.setRoles(List.of(rol));
        userDetails = new CustomUserDetails(u);
    }

    @Test
    @DisplayName("crear devuelve 201 con la valoración")
    void crear_ok() {
        CrearValoracionDTO dto = new CrearValoracionDTO();
        ValoracionDTO resultado = new ValoracionDTO();
        when(valoracionEstudianteService.crear(dto, 1L)).thenReturn(resultado);

        ResponseEntity<ValoracionDTO> resp = controller.crear(dto, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isEqualTo(resultado);
    }
}
