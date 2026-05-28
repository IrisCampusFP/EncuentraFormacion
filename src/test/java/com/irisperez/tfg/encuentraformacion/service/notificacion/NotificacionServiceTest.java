package com.irisperez.tfg.encuentraformacion.service.notificacion;

import com.irisperez.tfg.encuentraformacion.dto.notificacion.NotificacionDTO;
import com.irisperez.tfg.encuentraformacion.mapper.notificacion.NotificacionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Notificacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import com.irisperez.tfg.encuentraformacion.repository.MensajeRepository;
import com.irisperez.tfg.encuentraformacion.repository.NotificacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionService")
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private NotificacionMapper notificacionMapper;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificacionService service;

    @Test
    @DisplayName("crear: guarda la notificación y publica evento WebSocket")
    void crear_ok() {
        Usuario u = new Usuario(); u.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalseAndTipoNot(eq(1L), eq(TipoNotificacion.NUEVO_MENSAJE))).thenReturn(1L);
        when(mensajeRepository.countTotalNoLeidosPorUsuario(1L)).thenReturn(0L);

        assertThatCode(() -> service.crear(1L, TipoNotificacion.SOLICITUD_APROBADA,
            "Solicitud aprobada", "Tu solicitud fue aprobada", "/vistas/estudiante/mis-solicitudes.html"))
            .doesNotThrowAnyException();

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/usuario/1"), any(Object.class));
    }

    @Test
    @DisplayName("marcarLeida: 404 si la notificación no pertenece al usuario")
    void marcarLeida_ajena_lanza404() {
        when(notificacionRepository.findByIdAndUsuarioId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarLeida(1L, 2L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("countNoLeidas: delega al repositorio")
    void countNoLeidas_ok() {
        when(notificacionRepository.countByUsuarioIdAndLeidaFalseAndTipoNot(eq(1L), eq(TipoNotificacion.NUEVO_MENSAJE))).thenReturn(1L);
        when(mensajeRepository.countTotalNoLeidosPorUsuario(1L)).thenReturn(2L);

        assertThat(service.countNoLeidas(1L)).isEqualTo(3L);
    }

    @Test
    @DisplayName("getMisNotificaciones: retorna página mapeada")
    void getMisNotificaciones_ok() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notificacion> pageVacia = new PageImpl<>(List.of());
        when(notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(1L, pageable)).thenReturn(pageVacia);

        Page<NotificacionDTO> result = service.getMisNotificaciones(1L, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("marcarLeida: ok")
    void marcarLeida_ok() {
        Notificacion n = new Notificacion();
        n.setId(1L);
        n.setLeida(false);
        when(notificacionRepository.findByIdAndUsuarioId(1L, 1L)).thenReturn(Optional.of(n));

        service.marcarLeida(1L, 1L);

        assertThat(n.getLeida()).isTrue();
        verify(notificacionRepository).save(n);
    }

    @Test
    @DisplayName("marcarTodasLeidas: delega al repo")
    void marcarTodasLeidas() {
        service.marcarTodasLeidas(1L);
        verify(notificacionRepository).marcarTodasLeidasPorUsuario(1L);
    }

    @Test
    @DisplayName("marcarLeidasPorConversacion: marca por patrón de URL y publica count actualizado por WS")
    void marcarLeidasPorConversacion_ok() {
        when(notificacionRepository.countByUsuarioIdAndLeidaFalseAndTipoNot(eq(1L), eq(TipoNotificacion.NUEVO_MENSAJE))).thenReturn(0L);
        when(mensajeRepository.countTotalNoLeidosPorUsuario(1L)).thenReturn(0L);

        service.marcarLeidasPorConversacion(1L, 5L);

        verify(notificacionRepository).marcarLeidasPorUrlPatternYUsuario(eq(1L), contains("conv=5"));
        verify(messagingTemplate).convertAndSend(eq("/topic/usuario/1"), any(Object.class));
    }

    @Test
    @DisplayName("crearOActualizarMensaje: actualiza notificación existente con contenido corto")
    void crearOActualizarMensaje_existente_contenidoCorto() {
        String url = "/vistas/estudiante/chat.html?conv=5";
        Notificacion existente = new Notificacion();
        existente.setId(10L);
        existente.setTitulo("Viejo título");

        when(mensajeRepository.countNoLeidosPorDestinatario(5L, 1L)).thenReturn(1L);
        when(notificacionRepository.findFirstByUsuarioIdAndLeidaFalseAndUrlReferenciaOrderByFechaCreacionDesc(1L, url))
                .thenReturn(Optional.of(existente));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalseAndTipoNot(eq(1L), eq(TipoNotificacion.NUEVO_MENSAJE))).thenReturn(0L);
        when(mensajeRepository.countTotalNoLeidosPorUsuario(1L)).thenReturn(1L);

        service.crearOActualizarMensaje(1L, TipoNotificacion.NUEVO_MENSAJE, "Centro FP", "Hola", url, 5L);

        verify(notificacionRepository).save(existente);
        assertThat(existente.getTitulo()).contains("Nuevo mensaje de Centro FP");
        assertThat(existente.getMensaje()).isEqualTo("Hola");
        verify(messagingTemplate).convertAndSend(eq("/topic/usuario/1"), any(Object.class));
    }

    @Test
    @DisplayName("crearOActualizarMensaje: crea nueva notificación si no existe previa sin leer")
    void crearOActualizarMensaje_nueva_siNoExistente() {
        String url = "/vistas/estudiante/chat.html?conv=7";
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        when(mensajeRepository.countNoLeidosPorDestinatario(7L, 1L)).thenReturn(1L);
        when(notificacionRepository.findFirstByUsuarioIdAndLeidaFalseAndUrlReferenciaOrderByFechaCreacionDesc(1L, url))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalseAndTipoNot(eq(1L), eq(TipoNotificacion.NUEVO_MENSAJE))).thenReturn(0L);
        when(mensajeRepository.countTotalNoLeidosPorUsuario(1L)).thenReturn(1L);

        service.crearOActualizarMensaje(1L, TipoNotificacion.NUEVO_MENSAJE, "Gestor", "Mensaje nuevo", url, 7L);

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/usuario/1"), any(Object.class));
    }

    @Test
    @DisplayName("crearOActualizarMensaje: trunca a 57 caracteres con ellipsis contenido largo")
    void crearOActualizarMensaje_contenidoLargo_trunca() {
        String url = "/vistas/chat.html?conv=3";
        String contenidoLargo = "A".repeat(80);
        Notificacion existente = new Notificacion();

        when(mensajeRepository.countNoLeidosPorDestinatario(3L, 1L)).thenReturn(1L);
        when(notificacionRepository.findFirstByUsuarioIdAndLeidaFalseAndUrlReferenciaOrderByFechaCreacionDesc(1L, url))
                .thenReturn(Optional.of(existente));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalseAndTipoNot(eq(1L), eq(TipoNotificacion.NUEVO_MENSAJE))).thenReturn(0L);
        when(mensajeRepository.countTotalNoLeidosPorUsuario(1L)).thenReturn(0L);

        service.crearOActualizarMensaje(1L, TipoNotificacion.NUEVO_MENSAJE, "Centro", contenidoLargo, url, 3L);

        assertThat(existente.getMensaje()).endsWith("…");
        assertThat(existente.getMensaje()).hasSizeLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("crearOActualizarMensaje: múltiples mensajes no leídos cambia el título al plural")
    void crearOActualizarMensaje_variosNoLeidos_tituloPlural() {
        String url = "/vistas/chat.html?conv=2";
        Notificacion existente = new Notificacion();

        when(mensajeRepository.countNoLeidosPorDestinatario(2L, 1L)).thenReturn(3L);
        when(notificacionRepository.findFirstByUsuarioIdAndLeidaFalseAndUrlReferenciaOrderByFechaCreacionDesc(1L, url))
                .thenReturn(Optional.of(existente));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalseAndTipoNot(eq(1L), eq(TipoNotificacion.NUEVO_MENSAJE))).thenReturn(0L);
        when(mensajeRepository.countTotalNoLeidosPorUsuario(1L)).thenReturn(0L);

        service.crearOActualizarMensaje(1L, TipoNotificacion.NUEVO_MENSAJE, "Nombre", "Contenido", url, 2L);

        assertThat(existente.getTitulo()).contains("3 mensajes nuevos de Nombre");
    }
}
