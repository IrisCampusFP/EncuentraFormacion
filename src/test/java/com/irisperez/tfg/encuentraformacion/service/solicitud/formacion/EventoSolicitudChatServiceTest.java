package com.irisperez.tfg.encuentraformacion.service.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.dto.chat.EventoSolicitudChatDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.*;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoEventoSolicitud;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.ConversacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.EstudianteRepository;
import com.irisperez.tfg.encuentraformacion.repository.EventoSolicitudChatRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventoSolicitudChatService")
class EventoSolicitudChatServiceTest {

    @Mock private EventoSolicitudChatRepository eventoRepository;
    @Mock private ConversacionRepository conversacionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private CentroRepository centroRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private EventoSolicitudChatService service;

    private Estudiante estudiante;
    private Centro centro;
    private Conversacion conversacion;
    private SolicitudFormacion solicitud;

    @BeforeEach
    void setUp() {
        Usuario usuarioEst = new Usuario(); usuarioEst.setId(1L);
        estudiante = new Estudiante(); estudiante.setId(10L); estudiante.setUsuario(usuarioEst);

        Usuario usuarioGestor = new Usuario(); usuarioGestor.setId(2L);
        centro = new Centro(); centro.setId(20L);
        centro.setGestores(Set.of(usuarioGestor));

        Formacion formacion = new Formacion();
        formacion.setId(30L);
        formacion.setUuid(UUID.randomUUID());
        formacion.setNombre("DAM Presencial");
        formacion.setCentro(centro);

        conversacion = new Conversacion();
        conversacion.setId(100L);
        conversacion.setEstudiante(estudiante);
        conversacion.setCentro(centro);

        solicitud = new SolicitudFormacion();
        solicitud.setId(50L);
        solicitud.setEstudiante(estudiante);
        solicitud.setFormacion(formacion);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
    }

    @Nested
    @DisplayName("registrar()")
    class RegistrarTests {

        @Test
        @DisplayName("crea el evento y emite WS cuando existe conversacion")
        void registrar_conConversacion_creaEventoYEmiteWs() {
            when(conversacionRepository.findByEstudianteIdAndCentroId(10L, 20L))
                .thenReturn(Optional.of(conversacion));
            when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.registrar(solicitud, TipoEventoSolicitud.SOLICITUD_ENVIADA);

            verify(eventoRepository).save(any(EventoSolicitudChat.class));
            verify(messagingTemplate, times(2))
                .convertAndSend(any(String.class), (Object) any());
        }

        @Test
        @DisplayName("emite WS sin persistir evento si no existe conversacion")
        void registrar_sinConversacion_emiteWsSinPersistir() {
            when(conversacionRepository.findByEstudianteIdAndCentroId(10L, 20L))
                .thenReturn(Optional.empty());

            service.registrar(solicitud, TipoEventoSolicitud.SOLICITUD_ENVIADA);

            verifyNoInteractions(eventoRepository);
            verify(messagingTemplate, times(2))
                .convertAndSend(any(String.class), (Object) any());
        }
    }

    @Nested
    @DisplayName("getEventosEstudiante()")
    class GetEventosEstudianteTests {

        @Test
        @DisplayName("devuelve eventos de la conversacion propia")
        void getEventos_ok() {
            EventoSolicitudChat evento = buildEvento(TipoEventoSolicitud.SOLICITUD_ENVIADA);
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(conversacionRepository.findById(100L)).thenReturn(Optional.of(conversacion));
            when(eventoRepository.findByConversacionId(100L)).thenReturn(List.of(evento));

            List<EventoSolicitudChatDTO> result = service.getEventosEstudiante(100L, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTipoEvento()).isEqualTo("SOLICITUD_ENVIADA");
        }

        @Test
        @DisplayName("403 si la conversacion pertenece a otro estudiante")
        void getEventos_conversacionAjena_403() {
            Estudiante otro = new Estudiante(); otro.setId(99L);
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(otro));
            when(conversacionRepository.findById(100L)).thenReturn(Optional.of(conversacion));

            assertThatThrownBy(() -> service.getEventosEstudiante(100L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("getEventosGestor()")
    class GetEventosGestorTests {

        @Test
        @DisplayName("devuelve eventos de la conversacion del centro gestionado")
        void getEventos_ok() {
            EventoSolicitudChat evento = buildEvento(TipoEventoSolicitud.SOLICITUD_ACEPTADA);
            when(centroRepository.findByGestorId(2L)).thenReturn(Optional.of(centro));
            when(conversacionRepository.findById(100L)).thenReturn(Optional.of(conversacion));
            when(eventoRepository.findByConversacionId(100L)).thenReturn(List.of(evento));

            List<EventoSolicitudChatDTO> result = service.getEventosGestor(100L, 2L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTipoEvento()).isEqualTo("SOLICITUD_ACEPTADA");
        }

        @Test
        @DisplayName("403 si la conversacion pertenece a otro centro")
        void getEventos_conversacionAjena_403() {
            Centro otroCentro = new Centro(); otroCentro.setId(999L);
            when(centroRepository.findByGestorId(2L)).thenReturn(Optional.of(otroCentro));
            when(conversacionRepository.findById(100L)).thenReturn(Optional.of(conversacion));

            assertThatThrownBy(() -> service.getEventosGestor(100L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    private EventoSolicitudChat buildEvento(TipoEventoSolicitud tipo) {
        EventoSolicitudChat e = new EventoSolicitudChat();
        e.setId(1L);
        e.setConversacion(conversacion);
        e.setSolicitud(solicitud);
        e.setTipoEvento(tipo);
        e.setFormacionNombre("DAM Presencial");
        e.setFecha(LocalDateTime.now());
        return e;
    }
}
