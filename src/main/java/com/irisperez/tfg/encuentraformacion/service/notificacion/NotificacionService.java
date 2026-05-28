package com.irisperez.tfg.encuentraformacion.service.notificacion;

import com.irisperez.tfg.encuentraformacion.dto.notificacion.NotificacionDTO;
import com.irisperez.tfg.encuentraformacion.mapper.notificacion.NotificacionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Notificacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import com.irisperez.tfg.encuentraformacion.repository.MensajeRepository;
import com.irisperez.tfg.encuentraformacion.repository.NotificacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MensajeRepository mensajeRepository;
    private final NotificacionMapper notificacionMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // Crea una notificación para el destinatario — llamado desde Plan 3A
    @Transactional
    public void crear(Long usuarioDestinatarioId, TipoNotificacion tipo, String titulo, String mensaje, String urlReferencia) {
        Usuario usuario = usuarioRepository.findById(usuarioDestinatarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setUrlReferencia(urlReferencia);

        notificacionRepository.save(n);

        Map<String, Object> evento = Map.of("tipo", "notificacion", "count", countNoLeidas(usuarioDestinatarioId));
        messagingTemplate.convertAndSend("/topic/usuario/" + usuarioDestinatarioId, (Object) evento);
    }

    @Transactional(readOnly = true)
    public Page<NotificacionDTO> getMisNotificaciones(Long usuarioId, Pageable pageable) {
        return notificacionRepository
            .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId, pageable)
            .map(notificacionMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Long countNoLeidas(Long usuarioId) {
        long sistemicas = notificacionRepository.countByUsuarioIdAndLeidaFalseAndTipoNot(usuarioId, TipoNotificacion.NUEVO_MENSAJE);
        long mensajes = mensajeRepository.countTotalNoLeidosPorUsuario(usuarioId);
        return sistemicas + mensajes;
    }

    @Transactional
    public void marcarLeida(Long notificacionId, Long usuarioId) {
        Notificacion n = notificacionRepository.findByIdAndUsuarioId(notificacionId, usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));
        n.setLeida(true);
        notificacionRepository.save(n);
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        notificacionRepository.marcarTodasLeidasPorUsuario(usuarioId);
    }

    @Transactional
    public void crearOActualizarMensaje(Long usuarioDestinatarioId, TipoNotificacion tipo,
                                    String remitenteLabel, String contenido, String url, Long convId) {
        String preview = contenido != null && contenido.length() > 60
            ? contenido.substring(0, 57) + "…" : contenido;

        long noLeidosMensajes = mensajeRepository.countNoLeidosPorDestinatario(convId, usuarioDestinatarioId);
        String titulo = noLeidosMensajes > 1
            ? noLeidosMensajes + " mensajes nuevos de " + remitenteLabel
            : "Nuevo mensaje de " + remitenteLabel;

        Optional<Notificacion> existente = notificacionRepository
            .findFirstByUsuarioIdAndLeidaFalseAndUrlReferenciaOrderByFechaCreacionDesc(usuarioDestinatarioId, url);

        if (existente.isPresent()) {
            Notificacion n = existente.get();
            n.setTitulo(titulo);
            n.setMensaje(preview);
            notificacionRepository.save(n);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioDestinatarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            Notificacion n = new Notificacion();
            n.setUsuario(usuario);
            n.setTipo(tipo);
            n.setTitulo(titulo);
            n.setMensaje(preview);
            n.setUrlReferencia(url);
            notificacionRepository.save(n);
        }

        Map<String, Object> evento = Map.of("tipo", "notificacion", "count", countNoLeidas(usuarioDestinatarioId));
        messagingTemplate.convertAndSend("/topic/usuario/" + usuarioDestinatarioId, (Object) evento);
    }

    @Transactional
    public void marcarLeidasPorConversacion(Long usuarioId, Long convId) {
        notificacionRepository.marcarLeidasPorUrlPatternYUsuario(usuarioId, "%conv=" + convId);
        Map<String, Object> evento = Map.of("tipo", "notificacion", "count", countNoLeidas(usuarioId));
        messagingTemplate.convertAndSend("/topic/usuario/" + usuarioId, (Object) evento);
    }
}
