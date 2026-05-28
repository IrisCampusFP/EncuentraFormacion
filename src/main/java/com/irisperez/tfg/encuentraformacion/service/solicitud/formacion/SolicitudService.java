package com.irisperez.tfg.encuentraformacion.service.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.CrearSolicitudDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.SolicitudResumenDTO;
import com.irisperez.tfg.encuentraformacion.mapper.solicitud.formacion.SolicitudFormacionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoEventoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudFormacionRepository solicitudRepository;
    private final FormacionRepository formacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final SolicitudFormacionMapper mapper;
    private final EventoSolicitudChatService eventoSolicitudChatService;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public Page<SolicitudResumenDTO> getMisSolicitudes(Long usuarioId, EstadoSolicitud estado, Pageable pageable) {
        Estudiante est = getEstudiante(usuarioId);
        return solicitudRepository.findByEstudianteIdConFormacion(est.getId(), estado, pageable)
            .map(mapper::toDTO);
    }

    @Transactional
    public SolicitudResumenDTO crear(CrearSolicitudDTO dto, Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);

        Formacion formacion = formacionRepository.findByUuid(dto.getFormacionUuid())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formación no encontrada o inactiva"));

        if (Boolean.FALSE.equals(formacion.getCentro().getTieneGestor())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Este centro no tiene gestor asignado, no se pueden tramitar solicitudes");
        }

        if (solicitudRepository.existsByEstudianteIdAndFormacionIdAndEstado(
                est.getId(), formacion.getId(), EstadoSolicitud.PENDIENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes una solicitud pendiente para esta formación");
        }

        SolicitudFormacion sol = new SolicitudFormacion();
        sol.setEstudiante(est);
        sol.setFormacion(formacion);
        sol.setEstado(EstadoSolicitud.PENDIENTE);

        SolicitudFormacion saved = solicitudRepository.save(sol);
        eventoSolicitudChatService.registrar(saved, TipoEventoSolicitud.SOLICITUD_ENVIADA);

        String nombreEstudiante = est.getUsuario().getNombre() + " " + est.getUsuario().getApellidos();
        String mensajeNotif = nombreEstudiante.trim() + " ha enviado una solicitud para \"" + formacion.getNombre() + "\"";
        formacion.getCentro().getGestores().forEach(gestor ->
            notificacionService.crear(gestor.getId(), TipoNotificacion.NUEVA_SOLICITUD,
                "Nueva solicitud de admisión", mensajeNotif, "/vistas/gestor/solicitudes.html"));

        return mapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public SolicitudResumenDTO getById(Long solicitudId, Long usuarioId) {
        SolicitudFormacion sol = solicitudRepository
            .findByIdAndEstudiante_UsuarioId(solicitudId, usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        return mapper.toDTO(sol);
    }

    @Transactional(readOnly = true)
    public Optional<SolicitudResumenDTO> checkByFormacionUuid(UUID formacionUuid, Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);
        List<SolicitudFormacion> resultado =
            solicitudRepository.findByEstudianteIdAndFormacionUuid(est.getId(), formacionUuid);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(mapper.toDTO(resultado.get(0)));
    }

    @Transactional
    public void cancelar(Long solicitudId, Long usuarioId) {
        SolicitudFormacion sol = solicitudRepository
            .findByIdAndEstudiante_UsuarioId(solicitudId, usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (sol.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Solo se pueden cancelar solicitudes en estado PENDIENTE");
        }

        sol.setEstado(EstadoSolicitud.CANCELADA);
        solicitudRepository.save(sol);
        eventoSolicitudChatService.registrar(sol, TipoEventoSolicitud.SOLICITUD_CANCELADA);
    }

    private Estudiante getEstudiante(Long usuarioId) {
        return estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudiante no encontrado"));
    }
}
