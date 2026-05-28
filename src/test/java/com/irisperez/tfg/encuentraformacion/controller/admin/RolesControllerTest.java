package com.irisperez.tfg.encuentraformacion.controller.admin;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.RolDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.service.auth.RolService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolesController")
class RolesControllerTest {

    @Mock
    private RolService rolService;

    @InjectMocks
    private RolesController rolesController;

    private RolDTO rolAdmin;
    private RolDTO rolEstudiante;

    @BeforeEach
    void setUp() {
        rolAdmin = new RolDTO();
        rolAdmin.setId(1L);
        rolAdmin.setNombre(RolNombre.ADMIN);

        rolEstudiante = new RolDTO();
        rolEstudiante.setId(2L);
        rolEstudiante.setNombre(RolNombre.ESTUDIANTE);
    }

    @Test
    @DisplayName("obtenerRolesUsuario retorna lista de roles con estado 200 OK")
    void obtenerRolesUsuario_Retorna200ConListaDeRoles() {
        List<RolDTO> rolesMocheados = List.of(rolAdmin, rolEstudiante);
        when(rolService.obtenerRolesDTO()).thenReturn(rolesMocheados);

        ResponseEntity<?> response = rolesController.obtenerRolesUsuario();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(rolesMocheados);
        
        verify(rolService).obtenerRolesDTO();
    }
}
