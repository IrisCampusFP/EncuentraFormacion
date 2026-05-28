package com.irisperez.tfg.encuentraformacion.service.chat;

import com.irisperez.tfg.encuentraformacion.dto.chat.*;
import com.irisperez.tfg.encuentraformacion.mapper.chat.MensajeMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.*;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.repository.projection.ConversacionResumenProyeccion;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatEstudianteService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final EstudianteRepository estudianteRepository;
    private final CentroRepository centroRepository;
    private final FormacionRepository formacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MensajeMapper mensajeMapper;
    private final NotificacionService notificacionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ConversacionResumenDTO iniciarORecuperar(IniciarChatDTO dto, Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);
        Centro centro = centroRepository.findByUuid(dto.getCentroUuid())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centro no encontrado"));

        Formacion formacion = formacionRepository.findByUuid(dto.getFormacionUuid())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formación no encontrada"));
        if (!formacion.getCentro().getId().equals(centro.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La formación no pertenece al centro");
        }

        Conversacion conv = conversacionRepository
            .findByEstudianteIdAndCentroId(est.getId(), centro.getId())
            .orElseGet(() -> {
                Conversacion nueva = new Conversacion();
                nueva.setEstudiante(est);
                nueva.setCentro(centro);
                return conversacionRepository.save(nueva);
            });

        conv.getFormaciones().add(formacion);
        conversacionRepository.save(conv);

        return proyeccionToDTO(conv, usuarioId);
    }

    @Transactional(readOnly = true)
    public List<ConversacionResumenDTO> getMisConversaciones(Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);
        List<ConversacionResumenProyeccion> proyecciones =
            conversacionRepository.findResumenByEstudianteId(est.getId(), usuarioId);

        if (proyecciones.isEmpty()) return List.of();

        List<Long> ids = proyecciones.stream().map(ConversacionResumenProyeccion::getId).toList();
        Map<Long, List<FormacionChatDTO>> formacionesPorConv = conversacionRepository
            .findByIdsWithFormaciones(ids)
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                Conversacion::getId,
                c -> c.getFormaciones().stream().map(f -> {
                    FormacionChatDTO fdto = new FormacionChatDTO();
                    fdto.setId(f.getId());
                    fdto.setUuid(f.getUuid());
                    fdto.setNombre(f.getNombre());
                    fdto.setTipoEstudios(f.getTipoEstudios() != null ? f.getTipoEstudios().getNombre() : null);
                    return fdto;
                }).toList()
            ));

        return proyecciones.stream().map(p -> {
            ConversacionResumenDTO dto = new ConversacionResumenDTO();
            dto.setId(p.getId());
            dto.setCentroId(p.getCentroId());
            dto.setCentroUuid(p.getCentroUuid());
            dto.setCentroNombre(p.getCentroNombre());
            dto.setEstudianteId(p.getEstudianteId());
            dto.setEstudianteNombre(p.getEstudianteNombre());
            dto.setUltimaActividad(p.getUltimaActividad());
            dto.setNoLeidos(p.getNoLeidos());
            dto.setUltimoMensajeEsMio(p.getUltimoMensajeEsMio());
            String ult = p.getUltimoMensaje();
            if (ult != null) {
                dto.setUltimoMensaje(ult.length() > 80 ? ult.substring(0, 80) + "…" : ult);
            }
            dto.setFormaciones(formacionesPorConv.getOrDefault(p.getId(), List.of()));
            return dto;
        }).toList();
    }

    @Transactional
    public MensajeChatDTO enviarMensaje(Long conversacionId, EnviarMensajeDTO dto, Long usuarioId) {
        Conversacion conv = getConversacionDelEstudiante(conversacionId, usuarioId);
        Usuario remitente = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Mensaje m = new Mensaje();
        m.setConversacion(conv);
        m.setRemitente(remitente);
        m.setContenido(dto.getContenido());

        conv.setUltimaActividad(LocalDateTime.now());
        conversacionRepository.save(conv);

        Mensaje saved = mensajeRepository.save(m);

        // Notificar al gestor del centro
        conv.getCentro().getGestores().forEach(gestor -> {
            notificacionService.crearOActualizarMensaje(
                gestor.getId(), TipoNotificacion.NUEVO_MENSAJE,
                remitente.getNombre(), dto.getContenido(),
                "/vistas/gestor/chat.html?conv=" + conv.getId(),
                conv.getId());
            Map<String, Object> evento = Map.of("tipo", "nuevo_mensaje", "conversacionId", conv.getId());
            messagingTemplate.convertAndSend("/topic/usuario/" + gestor.getId(), (Object) evento);
        });

        return mensajeMapper.toDTO(saved);
    }

    @Transactional
    public ConversacionMensajesDTO getMensajes(Long conversacionId, Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);
        Conversacion conv = conversacionRepository.findByIdWithFormaciones(conversacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));
        if (!conv.getEstudiante().getId().equals(est.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta conversación");
        }

        mensajeRepository.marcarLeidosPorDestinatario(conversacionId, usuarioId);
        notificacionService.marcarLeidasPorConversacion(usuarioId, conversacionId);
        List<MensajeChatDTO> mensajes = mensajeMapper.toDTOList(
            mensajeRepository.findAllByConversacionId(conversacionId));

        ConversacionMensajesDTO resp = new ConversacionMensajesDTO();
        resp.setId(conv.getId());
        resp.setMensajes(mensajes);
        resp.setFormaciones(conv.getFormaciones().stream().map(f -> {
            FormacionChatDTO fdto = new FormacionChatDTO();
            fdto.setId(f.getId());
            fdto.setUuid(f.getUuid());
            fdto.setNombre(f.getNombre());
            fdto.setTipoEstudios(f.getTipoEstudios() != null ? f.getTipoEstudios().getNombre() : null);
            return fdto;
        }).toList());

        return resp;
    }

    @Transactional
    public void desvincularFormacion(Long conversacionId, java.util.UUID formacionUuid, Long usuarioId) {
        Conversacion conv = getConversacionDelEstudiante(conversacionId, usuarioId);

        if (conv.getFormaciones().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La conversación debe tener al menos una formación vinculada");
        }

        conv.getFormaciones().removeIf(f -> f.getUuid().equals(formacionUuid));
        conversacionRepository.save(conv);
    }

    private Conversacion getConversacionDelEstudiante(Long convId, Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);
        Conversacion conv = conversacionRepository.findById(convId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));
        if (!conv.getEstudiante().getId().equals(est.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta conversación");
        }
        return conv;
    }

    private ConversacionResumenDTO proyeccionToDTO(Conversacion c, Long usuarioId) {
        ConversacionResumenDTO dto = new ConversacionResumenDTO();
        dto.setId(c.getId());
        dto.setCentroId(c.getCentro().getId());
        dto.setCentroUuid(c.getCentro().getUuid());
        dto.setCentroNombre(c.getCentro().getNombreComercial());
        dto.setEstudianteId(c.getEstudiante().getId());
        dto.setEstudianteNombre(c.getEstudiante().getUsuario().getNombre());
        dto.setUltimaActividad(c.getUltimaActividad());
        dto.setNoLeidos(mensajeRepository.countNoLeidosPorDestinatario(c.getId(), usuarioId));
        return dto;
    }

    private Estudiante getEstudiante(Long usuarioId) {
        return estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudiante no encontrado"));
    }
}
