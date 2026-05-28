package com.irisperez.tfg.encuentraformacion.service.chat;

import com.irisperez.tfg.encuentraformacion.dto.chat.ConversacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.EnviarMensajeDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.MensajeChatDTO;
import com.irisperez.tfg.encuentraformacion.mapper.chat.MensajeMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.*;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatGestorServiceTest {

    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private CentroRepository centroRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MensajeMapper mensajeMapper;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatGestorService chatGestorService;

    private Centro centro;
    private Conversacion conversacion;
    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        centro = new Centro();
        centro.setId(1L);
        centro.setNombreComercial("Centro Test");

        Usuario u = new Usuario();
        u.setId(20L);
        u.setNombre("Est");
        u.setApellidos("Ape");

        estudiante = new Estudiante();
        estudiante.setId(30L);
        estudiante.setUsuario(u);

        conversacion = new Conversacion();
        conversacion.setId(100L);
        conversacion.setCentro(centro);
        conversacion.setEstudiante(estudiante);
    }

    @Test
    void getConversaciones_ok() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(conversacionRepository.findByCentroIdOrdenadas(1L)).thenReturn(List.of(conversacion));
        
        Usuario remitente = new Usuario();
        remitente.setId(20L);
        Mensaje ultimoMensaje = new Mensaje();
        ultimoMensaje.setContenido("Hola");
        ultimoMensaje.setFechaEnvio(LocalDateTime.now());
        ultimoMensaje.setRemitente(remitente);
        when(mensajeRepository.findUltimoMensaje(100L)).thenReturn(Optional.of(ultimoMensaje));
        when(mensajeRepository.countNoLeidosPorDestinatario(100L, 10L)).thenReturn(2L);

        List<ConversacionGestorDTO> result = chatGestorService.getConversaciones(10L, null);

        assertEquals(1, result.size());
        assertEquals("Hola", result.get(0).getUltimoMensaje());
        assertEquals(2L, result.get(0).getMensajesNoLeidos());
        assertEquals("Est Ape", result.get(0).getEstudianteNombre());
    }

    @Test
    void getMensajes_ok() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(conversacionRepository.findById(100L)).thenReturn(Optional.of(conversacion));
        when(mensajeRepository.findAllByConversacionId(100L)).thenReturn(List.of(new Mensaje()));
        when(mensajeMapper.toDTO(any())).thenReturn(new MensajeChatDTO());

        List<MensajeChatDTO> result = chatGestorService.getMensajes(10L, 100L);

        assertEquals(1, result.size());
        verify(mensajeRepository).marcarLeidosPorDestinatario(100L, 10L);
    }

    @Test
    void getMensajes_convDeOtroCentro_lanza404() {
        Centro otroCentro = new Centro();
        otroCentro.setId(2L);
        conversacion.setCentro(otroCentro);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(conversacionRepository.findById(100L)).thenReturn(Optional.of(conversacion));

        assertThrows(ResponseStatusException.class, () -> chatGestorService.getMensajes(10L, 100L));
    }

    @Test
    void enviarMensaje_ok() {
        EnviarMensajeDTO dto = new EnviarMensajeDTO();
        dto.setContenido("Respuesta");

        Usuario remitente = new Usuario();
        remitente.setId(10L);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(conversacionRepository.findById(100L)).thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(remitente));
        
        Mensaje saved = new Mensaje();
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(saved);
        when(mensajeMapper.toDTO(saved)).thenReturn(new MensajeChatDTO());

        chatGestorService.enviarMensaje(10L, 100L, dto);

        verify(mensajeRepository).save(argThat(m -> m.getContenido().equals("Respuesta")));
        verify(notificacionService).crearOActualizarMensaje(eq(20L), eq(TipoNotificacion.NUEVO_MENSAJE), anyString(), anyString(), anyString(), eq(100L));
        verify(messagingTemplate).convertAndSend(eq("/topic/usuario/20"), any(Object.class));
    }

    @Test
    void enviarMensaje_convDeOtroCentro_lanza404() {
        Centro otroCentro = new Centro();
        otroCentro.setId(2L);
        conversacion.setCentro(otroCentro);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(conversacionRepository.findById(100L)).thenReturn(Optional.of(conversacion));

        assertThrows(ResponseStatusException.class, () -> chatGestorService.enviarMensaje(10L, 100L, new EnviarMensajeDTO()));
    }
}
