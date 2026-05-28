package com.irisperez.tfg.encuentraformacion.service.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.CrearSolicitudDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.SolicitudResumenDTO;
import com.irisperez.tfg.encuentraformacion.mapper.solicitud.formacion.SolicitudFormacionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.*;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitudService")
class SolicitudServiceTest {

    @Mock private SolicitudFormacionRepository solicitudRepository;
    @Mock private FormacionRepository formacionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SolicitudFormacionMapper mapper;
    @Mock private EventoSolicitudChatService eventoSolicitudChatService;
    @Mock private NotificacionService notificacionService;

    @InjectMocks private SolicitudService service;

    private Estudiante estudiante;
    private Centro centro;
    private Formacion formacion;
    private UUID formacionUuid;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario(); usuario.setId(1L); usuario.setNombre("Test"); usuario.setApellidos("User");
        centro = new Centro(); centro.setId(10L); centro.setTieneGestor(true);
        estudiante = new Estudiante(); estudiante.setId(5L); estudiante.setUsuario(usuario);
        formacionUuid = UUID.randomUUID();
        formacion = new Formacion();
        formacion.setId(20L);
        formacion.setUuid(formacionUuid);
        formacion.setActiva(true);
        formacion.setCentro(centro);
    }

    @Nested
    @DisplayName("getMisSolicitudes()")
    class GetMisTests {
        @Test
        @DisplayName("devuelve página de solicitudes del estudiante")
        void getMisSolicitudes_ok() {
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            SolicitudFormacion sol = new SolicitudFormacion();
            Page<SolicitudFormacion> page = new PageImpl<>(List.of(sol));
            when(solicitudRepository.findByEstudianteIdConFormacion(eq(5L), any(), any())).thenReturn(page);
            SolicitudResumenDTO dto = new SolicitudResumenDTO();
            when(mapper.toDTO(sol)).thenReturn(dto);

            Page<SolicitudResumenDTO> result = service.getMisSolicitudes(1L, null, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("404 si el usuario no tiene perfil de estudiante")
        void getMisSolicitudes_sinEstudiante_lanza404() {
            when(estudianteRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMisSolicitudes(99L, null, PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("crear()")
    class CrearTests {

        @Test
        @DisplayName("crea solicitud correctamente")
        void crear_ok() {
            CrearSolicitudDTO dto = new CrearSolicitudDTO(); dto.setFormacionUuid(formacionUuid);
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(formacionRepository.findByUuid(formacionUuid)).thenReturn(Optional.of(formacion));
            when(solicitudRepository.existsByEstudianteIdAndFormacionIdAndEstado(5L, 20L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
            SolicitudFormacion saved = new SolicitudFormacion();
            when(solicitudRepository.save(any())).thenReturn(saved);
            SolicitudResumenDTO expected = new SolicitudResumenDTO();
            when(mapper.toDTO(saved)).thenReturn(expected);

            SolicitudResumenDTO result = service.crear(dto, 1L);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("409 si ya existe solicitud")
        void crear_duplicada_lanza409() {
            CrearSolicitudDTO dto = new CrearSolicitudDTO(); dto.setFormacionUuid(formacionUuid);
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(formacionRepository.findByUuid(formacionUuid)).thenReturn(Optional.of(formacion));
            when(solicitudRepository.existsByEstudianteIdAndFormacionIdAndEstado(5L, 20L, EstadoSolicitud.PENDIENTE)).thenReturn(true);

            assertThatThrownBy(() -> service.crear(dto, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("422 si el centro no tiene gestor")
        void crear_sinGestor_lanza422() {
            centro.setTieneGestor(false);
            CrearSolicitudDTO dto = new CrearSolicitudDTO(); dto.setFormacionUuid(formacionUuid);
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(formacionRepository.findByUuid(formacionUuid)).thenReturn(Optional.of(formacion));

            assertThatThrownBy(() -> service.crear(dto, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("404 si la formación no existe")
        void crear_formacionInexistente_lanza404() {
            UUID uuid = UUID.randomUUID();
            CrearSolicitudDTO dto = new CrearSolicitudDTO(); dto.setFormacionUuid(uuid);
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(formacionRepository.findByUuid(uuid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crear(dto, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("checkByFormacionUuid()")
    class CheckTests {

        @Test
        @DisplayName("devuelve solicitud si existe")
        void check_conSolicitud() {
            SolicitudFormacion sol = new SolicitudFormacion();
            SolicitudResumenDTO dto = new SolicitudResumenDTO();
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(solicitudRepository.findByEstudianteIdAndFormacionUuid(5L, formacionUuid))
                .thenReturn(List.of(sol));
            when(mapper.toDTO(sol)).thenReturn(dto);

            Optional<SolicitudResumenDTO> result = service.checkByFormacionUuid(formacionUuid, 1L);

            assertThat(result).isPresent().contains(dto);
        }

        @Test
        @DisplayName("devuelve vacío si no hay solicitud")
        void check_sinSolicitud() {
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(solicitudRepository.findByEstudianteIdAndFormacionUuid(5L, formacionUuid))
                .thenReturn(List.of());

            Optional<SolicitudResumenDTO> result = service.checkByFormacionUuid(formacionUuid, 1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("cancelar()")
    class CancelarTests {

        @Test
        @DisplayName("cancela solicitud PENDIENTE propia")
        void cancelar_pendiente_ok() {
            SolicitudFormacion sol = new SolicitudFormacion();
            sol.setId(1L); sol.setEstado(EstadoSolicitud.PENDIENTE);
            when(solicitudRepository.findByIdAndEstudiante_UsuarioId(1L, 1L)).thenReturn(Optional.of(sol));
            when(solicitudRepository.save(sol)).thenReturn(sol);

            assertThatCode(() -> service.cancelar(1L, 1L)).doesNotThrowAnyException();
            assertThat(sol.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
        }

        @Test
        @DisplayName("404 si la solicitud no pertenece al usuario")
        void cancelar_ajena_lanza404() {
            when(solicitudRepository.findByIdAndEstudiante_UsuarioId(1L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelar(1L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("409 si la solicitud ya fue resuelta")
        void cancelar_resuelta_lanza409() {
            SolicitudFormacion sol = new SolicitudFormacion();
            sol.setId(1L); sol.setEstado(EstadoSolicitud.ACEPTADA);
            when(solicitudRepository.findByIdAndEstudiante_UsuarioId(1L, 1L)).thenReturn(Optional.of(sol));

            assertThatThrownBy(() -> service.cancelar(1L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        }
    }
}
