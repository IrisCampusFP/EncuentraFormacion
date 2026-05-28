package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.valoracion.EditarValoracionDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.ValoracionDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValoracionEstudianteController — editar y miValoracion")
class EditarValoracionControllerTest {

    @Mock private ValoracionEstudianteService valoracionEstudianteService;
    @InjectMocks private ValoracionEstudianteController controller;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario();
        u.setId(1L);
        Rol rol = new Rol();
        rol.setNombre(RolNombre.ESTUDIANTE);
        u.setRoles(List.of(rol));
        userDetails = new CustomUserDetails(u);
    }

    @Test
    @DisplayName("editar devuelve 200 con la valoración actualizada")
    void editar_ok() {
        EditarValoracionDTO dto = new EditarValoracionDTO();
        dto.setEstrellas(4);
        ValoracionDTO resultado = new ValoracionDTO();
        when(valoracionEstudianteService.editar(10L, dto, 1L)).thenReturn(resultado);

        ResponseEntity<ValoracionDTO> resp = controller.editar(10L, dto, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(resultado);
    }

    @Test
    @DisplayName("editar propaga 403 del service")
    void editar_403_propagado() {
        EditarValoracionDTO dto = new EditarValoracionDTO();
        dto.setEstrellas(4);
        when(valoracionEstudianteService.editar(10L, dto, 1L))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> controller.editar(10L, dto, userDetails))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("miValoracion devuelve 200 si existe")
    void miValoracion_existe() {
        UUID uuid = UUID.randomUUID();
        ValoracionDTO dto = new ValoracionDTO();
        when(valoracionEstudianteService.miValoracion(uuid, 1L)).thenReturn(dto);

        ResponseEntity<ValoracionDTO> resp = controller.miValoracion(uuid, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("miValoracion devuelve 204 si no existe")
    void miValoracion_noExiste() {
        UUID uuid = UUID.randomUUID();
        when(valoracionEstudianteService.miValoracion(uuid, 1L)).thenReturn(null);

        ResponseEntity<ValoracionDTO> resp = controller.miValoracion(uuid, userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
