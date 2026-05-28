package com.irisperez.tfg.encuentraformacion.service.chat;

import com.irisperez.tfg.encuentraformacion.dto.chat.ConversacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.EnviarMensajeDTO;
import com.irisperez.tfg.encuentraformacion.dto.chat.MensajeChatDTO;
import com.irisperez.tfg.encuentraformacion.mapper.chat.MensajeMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.Conversacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Mensaje;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatGestorService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final CentroRepository centroRepository;
    private final UsuarioRepository usuarioRepository;
    private final MensajeMapper mensajeMapper;
    private final NotificacionService notificacionService;
    private final SimpMessagingTemplate messagingTemplate;

    public List<ConversacionGestorDTO> getConversaciones(Long usuarioId, Long formacionId) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        List<Conversacion> convs = formacionId != null
            ? conversacionRepository.findByCentroIdAndFormacionIdOrdenadas(centro.getId(), formacionId)
            : conversacionRepository.findByCentroIdOrdenadas(centro.getId());
        return convs.stream().map(c -> buildConversacionGestorDTO(c, usuarioId)).toList();
    }

    @Transactional
    public List<MensajeChatDTO> getMensajes(Long usuarioId, Long conversacionId) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        Conversacion conv = conversacionRepository.findById(conversacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));
        if (!conv.getCentro().getId().equals(centro.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada");
        }
        mensajeRepository.marcarLeidosPorDestinatario(conversacionId, usuarioId);
        notificacionService.marcarLeidasPorConversacion(usuarioId, conversacionId);
        return mensajeRepository.findAllByConversacionId(conversacionId)
            .stream().map(mensajeMapper::toDTO).toList();
    }

    @Transactional
    public MensajeChatDTO enviarMensaje(Long usuarioId, Long conversacionId, EnviarMensajeDTO dto) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        Conversacion conv = conversacionRepository.findById(conversacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));
        if (!conv.getCentro().getId().equals(centro.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada");
        }

        Usuario remitente = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conv);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(dto.getContenido());
        mensaje.setLeido(false);
        Mensaje saved = mensajeRepository.save(mensaje);

        // Notificar al estudiante
        Long estudianteUsuarioId = conv.getEstudiante().getUsuario().getId();
        notificacionService.crearOActualizarMensaje(
            estudianteUsuarioId, TipoNotificacion.NUEVO_MENSAJE,
            centro.getNombreComercial(), dto.getContenido(),
            "/vistas/estudiante/chat.html?conv=" + conv.getId(),
            conv.getId());
        Map<String, Object> evento = Map.of("tipo", "nuevo_mensaje", "conversacionId", conv.getId());
        messagingTemplate.convertAndSend("/topic/usuario/" + estudianteUsuarioId, (Object) evento);

        return mensajeMapper.toDTO(saved);
    }

    private ConversacionGestorDTO buildConversacionGestorDTO(Conversacion conv, Long gestorUsuarioId) {
        ConversacionGestorDTO dto = new ConversacionGestorDTO();
        dto.setId(conv.getId());
        dto.setEstudianteId(conv.getEstudiante().getId());
        dto.setEstudianteNombre(conv.getEstudiante().getUsuario().getNombre() + " " + conv.getEstudiante().getUsuario().getApellidos());
        mensajeRepository.findUltimoMensaje(conv.getId()).ifPresent(m -> {
            dto.setUltimoMensaje(m.getContenido());
            dto.setUltimaFecha(m.getFechaEnvio());
            dto.setUltimoMensajeEsMio(m.getRemitente() != null && m.getRemitente().getId().equals(gestorUsuarioId));
        });
        dto.setMensajesNoLeidos(mensajeRepository.countNoLeidosPorDestinatario(conv.getId(), gestorUsuarioId));
        
        List<FormacionResumenDTO> formacionesDto = conv.getFormaciones().stream().map(f -> {
            FormacionResumenDTO fdto = new FormacionResumenDTO();
            fdto.setId(f.getId());
            fdto.setNombre(f.getNombre());
            return fdto;
        }).toList();
        dto.setFormaciones(formacionesDto);

        return dto;
    }

    public long countMensajesNoLeidos(Long usuarioId) {
        return mensajeRepository.countTotalNoLeidosPorUsuario(usuarioId);
    }

    private Centro obtenerCentroDelGestor(Long usuarioId) {
        return centroRepository.findByGestorId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No gestionas ningún centro"));
    }
}
