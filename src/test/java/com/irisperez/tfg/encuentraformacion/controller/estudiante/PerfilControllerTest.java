package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.usuario.PerfilUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.auth.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerfilController")
class PerfilControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private PerfilController perfilController;

    private Authentication authentication;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("obtenerDatosUsuario()")
    class ObtenerDatosUsuario {

        @Test
        @DisplayName("usuario no autenticado retorna 401 UNAUTHORIZED")
        void usuarioNoAutenticado_Retorna401() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            ResponseEntity<?> response = perfilController.obtenerDatosUsuario();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNull();
            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("autenticación nula retorna 401 UNAUTHORIZED")
        void autenticacionNula_Retorna401() {
            when(securityContext.getAuthentication()).thenReturn(null);

            ResponseEntity<?> response = perfilController.obtenerDatosUsuario();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNull();
            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("usuario autenticado retorna 200 OK con sus datos")
        void usuarioAutenticado_Retorna200ConDTO() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("usuario@test.com");

            UsuarioDTO usuarioDTO = new UsuarioDTO();
            usuarioDTO.setEmail("usuario@test.com");
            usuarioDTO.setNombre("Test");
            when(usuarioService.obtenerUsuarioDTOPorEmail("usuario@test.com")).thenReturn(usuarioDTO);

            ResponseEntity<?> response = perfilController.obtenerDatosUsuario();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(usuarioDTO);
            verify(usuarioService).obtenerUsuarioDTOPorEmail("usuario@test.com");
        }

        @Test
        @DisplayName("estudiante autenticado incluye grado de estudios en el DTO")
        void estudianteAutenticado_IncludeGradoEstudios() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("estudiante@test.com");

            UsuarioDTO usuarioDTO = new UsuarioDTO();
            usuarioDTO.setEmail("estudiante@test.com");
            usuarioDTO.setGradoEstudiosId(4L);
            usuarioDTO.setGradoEstudiosNombre("Bachillerato");
            when(usuarioService.obtenerUsuarioDTOPorEmail("estudiante@test.com")).thenReturn(usuarioDTO);

            ResponseEntity<?> response = perfilController.obtenerDatosUsuario();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            UsuarioDTO body = (UsuarioDTO) response.getBody();
            assertThat(body.getGradoEstudiosId()).isEqualTo(4L);
            assertThat(body.getGradoEstudiosNombre()).isEqualTo("Bachillerato");
        }
    }

    @Nested
    @DisplayName("actualizarPerfil()")
    class ActualizarPerfil {

        @Test
        @DisplayName("actualiza datos comunes y devuelve 200")
        void actualizarPerfil_ok() {
            Usuario u = new Usuario(); u.setId(1L);
            Rol rol = new Rol(); rol.setNombre(RolNombre.ESTUDIANTE);
            u.setRoles(List.of(rol));
            CustomUserDetails userDetails = new CustomUserDetails(u);

            PerfilUpdateDTO dto = new PerfilUpdateDTO();
            UsuarioDTO resultado = new UsuarioDTO(); resultado.setEmail("test@test.com");
            when(usuarioService.actualizarPerfil(1L, dto)).thenReturn(resultado);

            ResponseEntity<UsuarioDTO> resp = perfilController.actualizarPerfil(dto, userDetails);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isEqualTo(resultado);
        }

        @Test
        @DisplayName("estudiante actualiza grado de estudios y el DTO lo refleja")
        void estudianteActualizaGrado_ok() {
            Usuario u = new Usuario(); u.setId(2L);
            Rol rol = new Rol(); rol.setNombre(RolNombre.ESTUDIANTE);
            u.setRoles(List.of(rol));
            CustomUserDetails userDetails = new CustomUserDetails(u);

            PerfilUpdateDTO dto = new PerfilUpdateDTO();
            dto.setGradoEstudiosId(3L);

            UsuarioDTO resultado = new UsuarioDTO();
            resultado.setGradoEstudiosId(3L);
            resultado.setGradoEstudiosNombre("Bachillerato");
            when(usuarioService.actualizarPerfil(2L, dto)).thenReturn(resultado);

            ResponseEntity<UsuarioDTO> resp = perfilController.actualizarPerfil(dto, userDetails);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody().getGradoEstudiosId()).isEqualTo(3L);
        }
    }

}
