package com.irisperez.tfg.encuentraformacion.controller.auth;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroGestorRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroUsuarioEstudianteRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.service.centro.CentroService;
import com.irisperez.tfg.encuentraformacion.service.solicitud.gestion.SolicitudGestionService;
import com.irisperez.tfg.encuentraformacion.service.auth.UsuarioService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistroController")
class RegistroControllerTest {

    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudGestionService solicitudGestionService;
    @Mock private CentroService centroService;

    @InjectMocks
    private RegistroController registroController;

    @Nested
    @DisplayName("registrarUsuario (Estudiante)")
    class RegistrarUsuario {

        @Test
        @DisplayName("datos válidos retorna 200 OK con el usuario DTO")
        void datosValidos_Retorna200YDTO() {
            RegistroUsuarioEstudianteRequestDTO requestDTO = new RegistroUsuarioEstudianteRequestDTO();
            UsuarioDTO usuarioDTO = new UsuarioDTO();
            usuarioDTO.setEmail("estudiante@test.com");

            when(usuarioService.registrarEstudiante(requestDTO)).thenReturn(usuarioDTO);

            ResponseEntity<?> response = registroController.registrarUsuario(requestDTO);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(usuarioDTO);
            verify(usuarioService).registrarEstudiante(requestDTO);
        }
    }

    @Nested
    @DisplayName("registrarGestor")
    class RegistrarGestor {

        private MultipartFile multipartFile;

        @BeforeEach
        void setUp() {
            multipartFile = mock(MultipartFile.class);
        }

        @Test
        @DisplayName("con ID de centro existente delega a solicitarGestorCentroExistente y retorna 200")
        void conIdCentroExistente_LlamaASolicitarExistente_Retorna200() throws Exception {
            RegistroGestorRequestDTO requestDTO = new RegistroGestorRequestDTO();
            requestDTO.setIdCentroExistente(100L);

            // Simula la llamada al controlador enviando una solicitud multipart y verifica la ruta de negocio correspondiente
            ResponseEntity<?> response = registroController.registrarGestor(requestDTO, multipartFile);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertThat(body).containsEntry("mensaje", "Usuario registrado y solicitud enviada con éxito.");

            verify(solicitudGestionService).registrarGestorCentroExistente(requestDTO, multipartFile);
            verify(solicitudGestionService, never()).registrarGestorCentroNuevo(any(), any());
        }

        @Test
        @DisplayName("sin ID de centro existente delega a solicitarGestorCentroNuevo y retorna 200")
        void sinIdCentroExistente_LlamaASolicitarNuevo_Retorna200() throws Exception {
            RegistroGestorRequestDTO requestDTO = new RegistroGestorRequestDTO();
            requestDTO.setIdCentroExistente(null);

            ResponseEntity<?> response = registroController.registrarGestor(requestDTO, multipartFile);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            verify(solicitudGestionService).registrarGestorCentroNuevo(requestDTO, multipartFile);
            verify(solicitudGestionService, never()).registrarGestorCentroExistente(any(), any());
        }
    }

    @Nested
    @DisplayName("registrarCentro")
    class RegistrarCentro {

        @Test
        @DisplayName("datos válidos retorna 201 CREATED con mensaje de éxito")
        void datosValidos_Retorna201ConMensaje() {
            RegistroCentroRequestDTO requestDTO = new RegistroCentroRequestDTO();

            when(centroService.registrarCentro(requestDTO)).thenReturn(new CentroDTO());

            ResponseEntity<?> response = registroController.registrarCentro(requestDTO);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertThat(body).containsEntry("mensaje", "Centro registrado correctamente pendiente de verificación.");

            verify(centroService).registrarCentro(requestDTO);
        }
    }

    @Nested
    @DisplayName("checkEmail")
    class CheckEmail {

        @Test
        @DisplayName("email existente retorna true")
        void emailExistente_RetornaTrue() {
            when(usuarioService.existeEmail("admin@test.com")).thenReturn(true);

            Map<String, Boolean> response = registroController.checkEmail("admin@test.com");

            assertThat(response).containsEntry("existe", true);
        }

        @Test
        @DisplayName("email no existente retorna false")
        void emailNoExistente_RetornaFalse() {
            when(usuarioService.existeEmail("nuevo@test.com")).thenReturn(false);

            Map<String, Boolean> response = registroController.checkEmail("nuevo@test.com");

            assertThat(response).containsEntry("existe", false);
        }
    }

    @Nested
    @DisplayName("checkUsername")
    class CheckUsername {

        @Test
        @DisplayName("username existente retorna true")
        void usernameExistente_RetornaTrue() {
            when(usuarioService.existeUsername("admin")).thenReturn(true);

            Map<String, Boolean> response = registroController.checkUsername("admin");

            assertThat(response).containsEntry("existe", true);
        }

        @Test
        @DisplayName("username no existente retorna false")
        void usernameNoExistente_RetornaFalse() {
            when(usuarioService.existeUsername("nuevo")).thenReturn(false);

            Map<String, Boolean> response = registroController.checkUsername("nuevo");

            assertThat(response).containsEntry("existe", false);
        }
    }
}
