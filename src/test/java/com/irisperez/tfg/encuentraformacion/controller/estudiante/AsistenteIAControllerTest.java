package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.asistente.EnviarMensajeIADTO;
import com.irisperez.tfg.encuentraformacion.dto.asistente.RespuestaAsistenteDTO;
import com.irisperez.tfg.encuentraformacion.dto.asistente.SesionIAResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.asistente.AsistenteIAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsistenteIAController")
class AsistenteIAControllerTest {

    @Mock private AsistenteIAService asistenteIAService;
    @InjectMocks private AsistenteIAController controller;

    private CustomUserDetails estudianteUser;
    private CustomUserDetails adminUser;
    private CustomUserDetails gestorUser;

    @BeforeEach
    void setUp() {
        estudianteUser = crearUser(1L, RolNombre.ESTUDIANTE);
        adminUser      = crearUser(2L, RolNombre.ADMIN);
        gestorUser     = crearUser(3L, RolNombre.GESTOR_CENTRO);
    }

    private CustomUserDetails crearUser(Long id, RolNombre rolNombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(rolNombre.name().toLowerCase() + "@test.com");
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        u.setRoles(List.of(rol));
        u.setActivo(true);
        return new CustomUserDetails(u);
    }

    // ── crearSesion ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crearSesion con rol ESTUDIANTE retorna 201")
    void crearSesion_conRolEstudiante_retorna201() {
        SesionIAResumenDTO dto = new SesionIAResumenDTO(1L, "Nueva conversación",
            LocalDateTime.now(), LocalDateTime.now(), 0L);
        when(asistenteIAService.crearSesion(1L)).thenReturn(dto);

        ResponseEntity<SesionIAResumenDTO> resp = controller.crearSesion(estudianteUser);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("crearSesion requiere @PreAuthorize con rol ESTUDIANTE")
    void crearSesion_requierePreAuthorizeEstudiante() throws NoSuchMethodException {
        Method m = AsistenteIAController.class.getMethod("crearSesion", CustomUserDetails.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ESTUDIANTE')");
    }

    // ── listarSesiones ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarSesiones devuelve lista para ESTUDIANTE")
    void listarSesiones_devuelveListaParaEstudiante() {
        when(asistenteIAService.listarSesiones(1L)).thenReturn(List.of());

        ResponseEntity<List<SesionIAResumenDTO>> resp = controller.listarSesiones(estudianteUser);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    @DisplayName("listarSesiones requiere @PreAuthorize con rol ESTUDIANTE")
    void listarSesiones_requierePreAuthorizeEstudiante() throws NoSuchMethodException {
        Method m = AsistenteIAController.class.getMethod("listarSesiones", CustomUserDetails.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ESTUDIANTE')");
    }

    // ── enviarMensaje ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("enviarMensaje devuelve 200 con respuesta del asistente")
    void enviarMensaje_devuelve200() {
        EnviarMensajeIADTO dto = new EnviarMensajeIADTO();
        dto.setContenido("Hola");
        RespuestaAsistenteDTO respDto = new RespuestaAsistenteDTO("Hola!", List.of());
        when(asistenteIAService.enviarMensaje(5L, "Hola", 1L)).thenReturn(respDto);

        ResponseEntity<RespuestaAsistenteDTO> resp = controller.enviarMensaje(5L, dto, estudianteUser);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getContenido()).isEqualTo("Hola!");
    }

    @Test
    @DisplayName("enviarMensaje requiere @PreAuthorize con rol ESTUDIANTE")
    void enviarMensaje_requierePreAuthorizeEstudiante() throws NoSuchMethodException {
        Method m = AsistenteIAController.class.getMethod(
            "enviarMensaje", Long.class, EnviarMensajeIADTO.class, CustomUserDetails.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ESTUDIANTE')");
    }

    // ── eliminarSesion ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminarSesion devuelve 204 para ESTUDIANTE")
    void eliminarSesion_devuelve204() {
        doNothing().when(asistenteIAService).eliminarSesion(5L, 1L);

        ResponseEntity<Void> resp = controller.eliminarSesion(5L, estudianteUser);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("eliminarSesion requiere @PreAuthorize con rol ESTUDIANTE")
    void eliminarSesion_requierePreAuthorizeEstudiante() throws NoSuchMethodException {
        Method m = AsistenteIAController.class.getMethod(
            "eliminarSesion", Long.class, CustomUserDetails.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ESTUDIANTE')");
    }
}
