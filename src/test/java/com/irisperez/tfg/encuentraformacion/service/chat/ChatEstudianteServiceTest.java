package com.irisperez.tfg.encuentraformacion.service.chat;

import com.irisperez.tfg.encuentraformacion.dto.chat.*;
import com.irisperez.tfg.encuentraformacion.mapper.chat.MensajeMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.*;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.repository.projection.ConversacionResumenProyeccion;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatEstudianteService")
class ChatEstudianteServiceTest {

    @Mock private ConversacionRepository conversacionRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private CentroRepository centroRepository;
    @Mock private FormacionRepository formacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MensajeMapper mensajeMapper;
    @Mock private NotificacionService notificacionService;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @InjectMocks private ChatEstudianteService service;

    private Estudiante estudiante;
    private Centro centro;
    private Formacion formacion;
    private UUID centroUuid;
    private UUID formacionUuid;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario(); u.setId(1L); u.setNombre("Ana");
        estudiante = new Estudiante(); estudiante.setId(5L); estudiante.setUsuario(u);
        centroUuid = UUID.randomUUID();
        formacionUuid = UUID.randomUUID();
        centro = new Centro(); centro.setId(10L); centro.setUuid(centroUuid); centro.setNombreComercial("Academia Test");
        formacion = new Formacion(); formacion.setId(100L); formacion.setUuid(formacionUuid); formacion.setCentro(centro); formacion.setNombre("Java Course");
    }

    @Test
    @DisplayName("iniciar_formacionNueva_seVinculaAConversacionExistente")
    void iniciar_formacionNueva_seVinculaAConversacionExistente() {
        IniciarChatDTO dto = new IniciarChatDTO(); dto.setCentroUuid(centroUuid); dto.setFormacionUuid(formacionUuid);
        Conversacion existente = new Conversacion(); existente.setId(2L);
        existente.setEstudiante(estudiante); existente.setCentro(centro);
        
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(centroRepository.findByUuid(centroUuid)).thenReturn(Optional.of(centro));
        when(formacionRepository.findByUuid(formacionUuid)).thenReturn(Optional.of(formacion));
        when(conversacionRepository.findByEstudianteIdAndCentroId(5L, 10L)).thenReturn(Optional.of(existente));
        when(mensajeRepository.countNoLeidosPorDestinatario(2L, 1L)).thenReturn(0L);

        ConversacionResumenDTO result = service.iniciarORecuperar(dto, 1L);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(existente.getFormaciones()).contains(formacion);
        verify(conversacionRepository).save(existente);
    }

    @Test
    @DisplayName("iniciar_formacionYaVinculada_noLanzaError")
    void iniciar_formacionYaVinculada_noLanzaError() {
        IniciarChatDTO dto = new IniciarChatDTO(); dto.setCentroUuid(centroUuid); dto.setFormacionUuid(formacionUuid);
        Conversacion existente = new Conversacion(); existente.setId(2L);
        existente.setEstudiante(estudiante); existente.setCentro(centro);
        existente.getFormaciones().add(formacion);
        
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(centroRepository.findByUuid(centroUuid)).thenReturn(Optional.of(centro));
        when(formacionRepository.findByUuid(formacionUuid)).thenReturn(Optional.of(formacion));
        when(conversacionRepository.findByEstudianteIdAndCentroId(5L, 10L)).thenReturn(Optional.of(existente));
        when(mensajeRepository.countNoLeidosPorDestinatario(2L, 1L)).thenReturn(0L);

        ConversacionResumenDTO result = service.iniciarORecuperar(dto, 1L);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(existente.getFormaciones()).hasSize(1);
        verify(conversacionRepository).save(existente);
    }

    @Test
    @DisplayName("iniciar_formacionDeOtroCentro_lanza400")
    void iniciar_formacionDeOtroCentro_lanza400() {
        IniciarChatDTO dto = new IniciarChatDTO(); dto.setCentroUuid(centroUuid); dto.setFormacionUuid(formacionUuid);
        Centro otroCentro = new Centro(); otroCentro.setId(99L);
        formacion.setCentro(otroCentro);
        
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(centroRepository.findByUuid(centroUuid)).thenReturn(Optional.of(centro));
        when(formacionRepository.findByUuid(formacionUuid)).thenReturn(Optional.of(formacion));

        assertThatThrownBy(() -> service.iniciarORecuperar(dto, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("desvincularFormacion_unica_lanza409")
    void desvincularFormacion_unica_lanza409() {
        Conversacion conv = new Conversacion(); conv.setId(1L); conv.setEstudiante(estudiante);
        conv.getFormaciones().add(formacion);
        
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.desvincularFormacion(1L, formacionUuid, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("desvincularFormacion_multiple_eliminaCorrectamente")
    void desvincularFormacion_multiple_eliminaCorrectamente() {
        Conversacion conv = new Conversacion(); conv.setId(1L); conv.setEstudiante(estudiante);
        conv.getFormaciones().add(formacion);
        Formacion formacion2 = new Formacion(); formacion2.setId(101L); formacion2.setUuid(UUID.randomUUID());
        conv.getFormaciones().add(formacion2);
        
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(conv));

        service.desvincularFormacion(1L, formacionUuid, 1L);

        assertThat(conv.getFormaciones()).hasSize(1);
        assertThat(conv.getFormaciones()).doesNotContain(formacion);
        verify(conversacionRepository).save(conv);
    }

    @Test
    @DisplayName("desvincularFormacion_conversacionAjena_lanza403")
    void desvincularFormacion_conversacionAjena_lanza403() {
        Estudiante otroEstudiante = new Estudiante(); otroEstudiante.setId(99L);
        Conversacion conv = new Conversacion(); conv.setId(1L); conv.setEstudiante(otroEstudiante);
        
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.desvincularFormacion(1L, formacionUuid, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("getMisConversaciones: devuelve lista mapeada desde proyección")
    void getMisConversaciones() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));

        ConversacionResumenProyeccion proyeccion = mock(ConversacionResumenProyeccion.class);
        when(proyeccion.getId()).thenReturn(100L);
        when(proyeccion.getCentroId()).thenReturn(10L);
        when(proyeccion.getCentroNombre()).thenReturn("Academia Test");
        when(proyeccion.getEstudianteId()).thenReturn(5L);
        when(proyeccion.getEstudianteNombre()).thenReturn("Ana");
        when(proyeccion.getUltimaActividad()).thenReturn(null);
        when(proyeccion.getNoLeidos()).thenReturn(0L);
        when(proyeccion.getUltimoMensaje()).thenReturn(null);

        when(conversacionRepository.findResumenByEstudianteId(5L, 1L)).thenReturn(List.of(proyeccion));

        List<ConversacionResumenDTO> result = service.getMisConversaciones(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(0).getNoLeidos()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getMensajes: 403 si la conversación no pertenece al estudiante")
    void getMensajes_conversacionAjena_lanza403() {
        Estudiante otroEstudiante = new Estudiante(); otroEstudiante.setId(99L);
        Conversacion conv = new Conversacion(); conv.setId(1L); conv.setEstudiante(otroEstudiante);

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findByIdWithFormaciones(1L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.getMensajes(1L, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("getMensajes: devuelve página de mensajes, formaciones y marca como leídos")
    void getMensajes() {
        Conversacion conv = new Conversacion(); conv.setId(1L); conv.setEstudiante(estudiante);
        conv.getFormaciones().add(formacion);

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findByIdWithFormaciones(1L)).thenReturn(Optional.of(conv));

        Mensaje m = new Mensaje(); m.setId(10L);
        when(mensajeRepository.findAllByConversacionId(1L)).thenReturn(List.of(m));

        MensajeChatDTO dto = new MensajeChatDTO(); dto.setId(10L);
        when(mensajeMapper.toDTOList(List.of(m))).thenReturn(List.of(dto));

        ConversacionMensajesDTO result = service.getMensajes(1L, 1L);

        assertThat(result.getMensajes()).hasSize(1);
        assertThat(result.getMensajes().get(0).getId()).isEqualTo(10L);
        assertThat(result.getFormaciones()).hasSize(1);
        verify(mensajeRepository).marcarLeidosPorDestinatario(1L, 1L);
    }

    @Test
    @DisplayName("enviarMensaje: guarda el mensaje y actualiza ultima actividad")
    void enviarMensaje() {
        Conversacion conv = new Conversacion(); conv.setId(1L);
        conv.setEstudiante(estudiante); conv.setCentro(centro);

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(conv));

        Usuario remitente = new Usuario(); remitente.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(remitente));

        EnviarMensajeDTO dto = new EnviarMensajeDTO(); dto.setContenido("Hola!");

        Mensaje mGuardado = new Mensaje(); mGuardado.setId(10L);
        when(mensajeRepository.save(any())).thenReturn(mGuardado);

        MensajeChatDTO respDto = new MensajeChatDTO(); respDto.setId(10L);
        when(mensajeMapper.toDTO(mGuardado)).thenReturn(respDto);

        MensajeChatDTO result = service.enviarMensaje(1L, dto, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        verify(conversacionRepository).save(conv);
        verify(mensajeRepository).save(any(Mensaje.class));
    }
}
