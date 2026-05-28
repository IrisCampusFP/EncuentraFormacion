package com.irisperez.tfg.encuentraformacion.service.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.CambiarEstadoSolicitudDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestorDTO;
import com.irisperez.tfg.encuentraformacion.mapper.solicitud.gestion.SolicitudGestorMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoEventoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.SolicitudFormacionRepository;
import java.time.LocalDateTime;

import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GestorSolicitudService {

    private final SolicitudFormacionRepository solicitudRepository;
    private final CentroRepository centroRepository;
    private final SolicitudGestorMapper mapper;
    private final NotificacionService notificacionService;
    private final EventoSolicitudChatService eventoSolicitudChatService;

    @Transactional(readOnly = true)
    public Page<SolicitudGestorDTO> getSolicitudes(Long usuarioId, Long formacionId,
            java.util.List<EstadoSolicitud> estados, String nombreEstudiante, Pageable pageable) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        String nombre = (nombreEstudiante != null && !nombreEstudiante.isBlank()) ? nombreEstudiante : "";
        return solicitudRepository.findByCentroIdWithFilters(centro.getId(), formacionId, estados, nombre, pageable)
            .map(mapper::toDTO);
    }

    @Transactional
    public SolicitudGestorDTO cambiarEstado(Long usuarioId, Long solicitudId, CambiarEstadoSolicitudDTO dto) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        SolicitudFormacion solicitud = solicitudRepository.findByIdAndCentroId(solicitudId, centro.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden modificar solicitudes en estado PENDIENTE");
        }
        if (dto.getEstado() != EstadoSolicitud.ACEPTADA && dto.getEstado() != EstadoSolicitud.RECHAZADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido. Solo ACEPTADA o RECHAZADA");
        }

        solicitud.setEstado(dto.getEstado());
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        // Notificar al estudiante
        Long estudianteUsuarioId = solicitud.getEstudiante().getUsuario().getId();
        TipoNotificacion tipo = dto.getEstado() == EstadoSolicitud.ACEPTADA
            ? TipoNotificacion.SOLICITUD_APROBADA
            : TipoNotificacion.SOLICITUD_RECHAZADA;
        String formacionNombre = solicitud.getFormacion().getNombre();
        String titulo = "Estado de tu solicitud actualizado";
        String mensaje = dto.getEstado() == EstadoSolicitud.ACEPTADA
            ? "Tu solicitud para \"" + formacionNombre + "\" ha sido aceptada"
            : "Tu solicitud para \"" + formacionNombre + "\" ha sido rechazada";
        notificacionService.crear(estudianteUsuarioId, tipo, titulo, mensaje,
            "/vistas/estudiante/mis-solicitudes.html");

        TipoEventoSolicitud tipoEvento = dto.getEstado() == EstadoSolicitud.ACEPTADA
            ? TipoEventoSolicitud.SOLICITUD_ACEPTADA
            : TipoEventoSolicitud.SOLICITUD_RECHAZADA;
        eventoSolicitudChatService.registrar(solicitud, tipoEvento);

        return mapper.toDTO(solicitud);
    }

    @Transactional(readOnly = true)
    public SolicitudGestorDTO getById(Long usuarioId, Long solicitudId) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        SolicitudFormacion solicitud = solicitudRepository.findByIdAndCentroId(solicitudId, centro.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        return mapper.toDTO(solicitud);
    }

    @Transactional(readOnly = true)
    public long countPendientes(Long usuarioId) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        return solicitudRepository.countByCentroIdAndEstado(centro.getId(), EstadoSolicitud.PENDIENTE);
    }

    @Transactional(readOnly = true)
    public long countByEstado(Long usuarioId, EstadoSolicitud estado) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        return solicitudRepository.countByCentroIdAndEstado(centro.getId(), estado);
    }

    @Transactional(readOnly = true)
    public long countByEstadoWithFilters(Long usuarioId, EstadoSolicitud estado,
            Long formacionId, String nombreEstudiante) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        String nombre = (nombreEstudiante != null && !nombreEstudiante.isBlank()) ? nombreEstudiante : "";
        return solicitudRepository.countByCentroIdWithFilters(centro.getId(), estado, formacionId, nombre);
    }

    private Centro obtenerCentroDelGestor(Long usuarioId) {
        return centroRepository.findByGestorId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No gestionas ningún centro"));
    }
}
