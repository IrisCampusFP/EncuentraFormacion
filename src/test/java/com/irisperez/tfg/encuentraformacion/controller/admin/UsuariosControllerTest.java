package com.irisperez.tfg.encuentraformacion.controller.admin;

import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioUpdateDTO;
import com.irisperez.tfg.encuentraformacion.service.auth.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuariosController")
class UsuariosControllerTest {

    @Mock private UsuarioService usuarioService;

    @InjectMocks
    private UsuariosController usuariosController;

    private UsuarioDTO usuarioDTO;
    private RegistroRequestDTO registroDTO;

    @BeforeEach
    void setUp() {
        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setId(1L);
        usuarioDTO.setEmail("ana@test.com");
        usuarioDTO.setNombre("Ana");
        usuarioDTO.setApellidos("García");
        usuarioDTO.setUsername("ana99");
        usuarioDTO.setActivo(true);
        usuarioDTO.setFechaAlta(LocalDateTime.now());
        usuarioDTO.setRoles(new ArrayList<>());

        registroDTO = new RegistroRequestDTO();
        registroDTO.setEmail("ana@test.com");
        registroDTO.setNombre("Ana");
        registroDTO.setApellidos("García");
        registroDTO.setUsername("ana99");
        registroDTO.setPassword("Password1");
    }

    @Nested
    @DisplayName("crearUsuario()")
    class CrearUsuario {

        @Test
        @DisplayName("DTO válido retorna 201 con el usuario creado")
        void dtoValido_retorna201() {
            // Simula la creación y valida que el estado HTTP sea 201 CREATED
            when(usuarioService.registrarUsuario(any())).thenReturn(usuarioDTO);

            ResponseEntity<UsuarioDTO> response = usuariosController.crearUsuario(registroDTO);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(usuarioDTO);
            verify(usuarioService).registrarUsuario(registroDTO);
        }

        @Test
        @DisplayName("email duplicado propaga IllegalArgumentException")
        void emailDuplicado_propagaIllegalArgumentException() {
            when(usuarioService.registrarUsuario(any()))
                    .thenThrow(new IllegalArgumentException("Ya existe un usuario con ese email."));

            assertThatThrownBy(() -> usuariosController.crearUsuario(registroDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un usuario con ese email.");
        }
    }

    @Nested
    @DisplayName("obtenerUsuarios()")
    class ObtenerUsuarios {

        @Test
        @DisplayName("retorna 200 con la página de usuarios")
        void retornaPagina_con200() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<UsuarioDTO> page = new PageImpl<>(List.of(usuarioDTO));
            when(usuarioService.buscarConFiltros(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

            ResponseEntity<Page<UsuarioDTO>> response = usuariosController.obtenerUsuarios(pageable, null, null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).getEmail()).isEqualTo("ana@test.com");
        }

        @Test
        @DisplayName("sin usuarios retorna 200 con página vacía")
        void sinUsuarios_retorna200PaginaVacia() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<UsuarioDTO> page = new PageImpl<>(List.of());
            when(usuarioService.buscarConFiltros(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

            ResponseEntity<Page<UsuarioDTO>> response = usuariosController.obtenerUsuarios(pageable, null, null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerUsuarioPorId()")
    class ObtenerUsuarioPorId {

        @Test
        @DisplayName("id existente retorna 200 con el usuario")
        void idExistente_retorna200() {
            when(usuarioService.obtenerUsuarioDTOPorId(1L)).thenReturn(usuarioDTO);

            ResponseEntity<UsuarioDTO> response = usuariosController.obtenerUsuarioPorId(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(usuarioDTO);
        }

        @Test
        @DisplayName("id inexistente propaga IllegalArgumentException")
        void idInexistente_propagaIllegalArgumentException() {
            when(usuarioService.obtenerUsuarioDTOPorId(999L))
                    .thenThrow(new IllegalArgumentException("No se ha encontrado ningún usuario con id: 999"));

            assertThatThrownBy(() -> usuariosController.obtenerUsuarioPorId(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("actualizarUsuario()")
    class ActualizarUsuario {

        @Test
        @DisplayName("datos válidos retorna 200 con usuario actualizado")
        void datosValidos_retorna200() {
            UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
            updateDTO.setEmail("ana@test.com");
            updateDTO.setNombre("Ana Modificada");
            updateDTO.setApellidos("García");
            updateDTO.setUsername("ana99");

            usuarioDTO.setNombre("Ana Modificada");
            when(usuarioService.actualizarUsuario(eq(1L), any())).thenReturn(usuarioDTO);

            ResponseEntity<UsuarioDTO> response = usuariosController.actualizarUsuario(1L, updateDTO);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getNombre()).isEqualTo("Ana Modificada");
        }
    }

    @Nested
    @DisplayName("cambiarEstadoUsuario()")
    class CambiarEstado {

        @Test
        @DisplayName("desactivar usuario retorna 200 con activo=false")
        void desactivar_retorna200ConActivoFalse() {
            when(usuarioService.actualizarEstado(1L, false)).thenReturn(false);

            ResponseEntity<Map<String, Boolean>> response =
                    usuariosController.cambiarEstadoUsuario(1L, false);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("activo", false);
        }

        @Test
        @DisplayName("activar usuario retorna 200 con activo=true")
        void activar_retorna200ConActivoTrue() {
            when(usuarioService.actualizarEstado(1L, true)).thenReturn(true);

            ResponseEntity<Map<String, Boolean>> response =
                    usuariosController.cambiarEstadoUsuario(1L, true);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("activo", true);
        }
    }

    @Nested
    @DisplayName("eliminarUsuario()")
    class EliminarUsuario {

        @Test
        @DisplayName("id existente retorna 200 e invoca el borrado")
        void idExistente_retorna200() {
            // Verifica que el servicio de eliminación se haya llamado exactamente una vez con ese ID
            doNothing().when(usuarioService).eliminarUsuario(1L);

            ResponseEntity<Void> response = usuariosController.eliminarUsuario(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(usuarioService).eliminarUsuario(1L);
        }

        @Test
        @DisplayName("id inexistente propaga IllegalStateException")
        void idInexistente_propagaIllegalStateException() {
            doThrow(new IllegalStateException("No se ha encontrado ningún usuario con id: 999"))
                    .when(usuarioService).eliminarUsuario(999L);

            assertThatThrownBy(() -> usuariosController.eliminarUsuario(999L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("cambiarPasswordUsuario()")
    class CambiarPassword {

        @Test
        @DisplayName("contraseña válida retorna 200 e invoca el servicio")
        void passwordValida_retorna200() {
            doNothing().when(usuarioService).cambiarPasswordAdmin(eq(1L), anyString());

            ResponseEntity<Void> response = usuariosController.cambiarPasswordUsuario(
                    1L, Map.of("passwordNueva", "NuevoPass1"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(usuarioService).cambiarPasswordAdmin(1L, "NuevoPass1");
        }
    }

    @Nested
    @DisplayName("actualizarRolesUsuario()")
    class ActualizarRoles {

        @Test
        @DisplayName("lista válida retorna 200 e invoca el servicio")
        void rolesValidos_retorna200() {
            doNothing().when(usuarioService).actualizarRolesUsuario(eq(1L), anyList());

            ResponseEntity<Void> response =
                    usuariosController.actualizarRolesUsuario(1L, List.of(1L, 2L));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(usuarioService).actualizarRolesUsuario(1L, List.of(1L, 2L));
        }

        @Test
        @DisplayName("lista vacía propaga IllegalArgumentException")
        void listaVacia_propagaIllegalArgumentException() {
            doThrow(new IllegalArgumentException("Un usuario debe tener al menos un rol."))
                    .when(usuarioService).actualizarRolesUsuario(eq(1L), anyList());

            assertThatThrownBy(() -> usuariosController.actualizarRolesUsuario(1L, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("al menos un rol");
        }
    }
}
