package com.irisperez.tfg.encuentraformacion.service.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.dto.chat.EventoSolicitudChatDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.*;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoEventoSolicitud;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.ConversacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.EstudianteRepository;
import com.irisperez.tfg.encuentraformacion.repository.EventoSolicitudChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventoSolicitudChatService {

    private final EventoSolicitudChatRepository eventoRepository;
    private final ConversacionRepository conversacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final CentroRepository centroRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void registrar(SolicitudFormacion solicitud, TipoEventoSolicitud tipo) {
        Estudiante estudiante = solicitud.getEstudiante();
        Centro centro = solicitud.getFormacion().getCentro();

        Optional<Conversacion> convOpt = conversacionRepository
            .findByEstudianteIdAndCentroId(estudiante.getId(), centro.getId());

        EventoSolicitudChatDTO dto;
        Map<String, Object> payload;

        if (convOpt.isPresent()) {
            Conversacion conv = convOpt.get();
            EventoSolicitudChat evento = new EventoSolicitudChat();
            evento.setConversacion(conv);
            evento.setSolicitud(solicitud);
            evento.setTipoEvento(tipo);
            evento.setFormacionNombre(solicitud.getFormacion().getNombre());
            dto = toDTO(eventoRepository.save(evento));
            payload = Map.of("tipo", "solicitud_evento", "conversacionId", conv.getId(), "evento", dto);
        } else {
            dto = buildDTO(solicitud, tipo);
            payload = Map.of("tipo", "solicitud_evento", "evento", dto);
        }

        messagingTemplate.convertAndSend(
            "/topic/usuario/" + estudiante.getUsuario().getId(), (Object) payload);
        centro.getGestores().forEach(gestor ->
            messagingTemplate.convertAndSend(
                "/topic/usuario/" + gestor.getId(), (Object) payload));
    }

    @Transactional(readOnly = true)
    public List<EventoSolicitudChatDTO> getEventosEstudiante(Long conversacionId, Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Conversacion conv = conversacionRepository.findById(conversacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!conv.getEstudiante().getId().equals(est.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return eventoRepository.findByConversacionId(conversacionId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EventoSolicitudChatDTO> getEventosGestor(Long conversacionId, Long usuarioId) {
        Centro centro = centroRepository.findByGestorId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Conversacion conv = conversacionRepository.findById(conversacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!conv.getCentro().getId().equals(centro.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return eventoRepository.findByConversacionId(conversacionId).stream().map(this::toDTO).toList();
    }

    private EventoSolicitudChatDTO toDTO(EventoSolicitudChat e) {
        EventoSolicitudChatDTO dto = new EventoSolicitudChatDTO();
        dto.setId(e.getId());
        dto.setSolicitudId(e.getSolicitud().getId());
        dto.setTipoEvento(e.getTipoEvento().name());
        dto.setFormacionNombre(e.getFormacionNombre());
        dto.setFormacionUuid(e.getSolicitud().getFormacion().getUuid());
        dto.setFecha(e.getFecha());
        return dto;
    }

    private EventoSolicitudChatDTO buildDTO(SolicitudFormacion solicitud, TipoEventoSolicitud tipo) {
        EventoSolicitudChatDTO dto = new EventoSolicitudChatDTO();
        dto.setSolicitudId(solicitud.getId());
        dto.setTipoEvento(tipo.name());
        dto.setFormacionNombre(solicitud.getFormacion().getNombre());
        dto.setFormacionUuid(solicitud.getFormacion().getUuid());
        dto.setFecha(LocalDateTime.now());
        return dto;
    }
}
