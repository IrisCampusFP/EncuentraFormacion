package com.irisperez.tfg.encuentraformacion.service.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.CambiarEstadoSolicitudDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestorDTO;
import com.irisperez.tfg.encuentraformacion.mapper.solicitud.gestion.SolicitudGestorMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.*;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.SolicitudFormacionRepository;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorSolicitudServiceTest {

    @Mock
    private SolicitudFormacionRepository solicitudRepository;
    @Mock
    private CentroRepository centroRepository;
    @Mock
    private SolicitudGestorMapper mapper;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private EventoSolicitudChatService eventoSolicitudChatService;

    @InjectMocks
    private GestorSolicitudService gestorSolicitudService;

    private Centro centro;
    private SolicitudFormacion solicitud;

    @BeforeEach
    void setUp() {
        centro = new Centro();
        centro.setId(1L);

        Usuario usuarioEstudiante = new Usuario();
        usuarioEstudiante.setId(20L);

        Estudiante estudiante = new Estudiante();
        estudiante.setUsuario(usuarioEstudiante);

        Formacion formacion = new Formacion();
        formacion.setNombre("Curso Test");

        solicitud = new SolicitudFormacion();
        solicitud.setId(100L);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setEstudiante(estudiante);
        solicitud.setFormacion(formacion);
    }

    @Test
    void countPendientes_devuelveCountDelRepositorio() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(solicitudRepository.countByCentroIdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(3L);

        long result = gestorSolicitudService.countPendientes(10L);

        assertEquals(3L, result);
    }

    @Test
    void getSolicitudes_ok() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        Page<SolicitudFormacion> page = new PageImpl<>(List.of(solicitud));
        when(solicitudRepository.findByCentroIdWithFilters(eq(1L), any(), any(), any(), any())).thenReturn(page);
        when(mapper.toDTO(any())).thenReturn(new SolicitudGestorDTO());

        Page<SolicitudGestorDTO> result = gestorSolicitudService.getSolicitudes(10L, null, null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void cambiarEstado_aAceptada_ok() {
        CambiarEstadoSolicitudDTO dto = new CambiarEstadoSolicitudDTO();
        dto.setEstado(EstadoSolicitud.ACEPTADA);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(solicitudRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.of(solicitud));
        when(mapper.toDTO(solicitud)).thenReturn(new SolicitudGestorDTO());

        gestorSolicitudService.cambiarEstado(10L, 100L, dto);

        assertEquals(EstadoSolicitud.ACEPTADA, solicitud.getEstado());
        assertNotNull(solicitud.getFechaRespuesta());
        verify(notificacionService).crear(eq(20L), eq(TipoNotificacion.SOLICITUD_APROBADA), anyString(), anyString(), anyString());
    }

    @Test
    void cambiarEstado_aRechazada_ok() {
        CambiarEstadoSolicitudDTO dto = new CambiarEstadoSolicitudDTO();
        dto.setEstado(EstadoSolicitud.RECHAZADA);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(solicitudRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.of(solicitud));
        when(mapper.toDTO(solicitud)).thenReturn(new SolicitudGestorDTO());

        gestorSolicitudService.cambiarEstado(10L, 100L, dto);

        assertEquals(EstadoSolicitud.RECHAZADA, solicitud.getEstado());
        assertNotNull(solicitud.getFechaRespuesta());
        verify(notificacionService).crear(eq(20L), eq(TipoNotificacion.SOLICITUD_RECHAZADA), anyString(), anyString(), anyString());
    }

    @Test
    void cambiarEstado_yaNoEsPendiente_lanza409() {
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        CambiarEstadoSolicitudDTO dto = new CambiarEstadoSolicitudDTO();
        dto.setEstado(EstadoSolicitud.RECHAZADA);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(solicitudRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.of(solicitud));

        assertThrows(ResponseStatusException.class, () -> gestorSolicitudService.cambiarEstado(10L, 100L, dto));
    }

    @Test
    void cambiarEstado_solicitudDeOtroCentro_lanza404() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(solicitudRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gestorSolicitudService.cambiarEstado(10L, 100L, new CambiarEstadoSolicitudDTO()));
    }

    @Test
    void cambiarEstado_estadoInvalido_lanza400() {
        CambiarEstadoSolicitudDTO dto = new CambiarEstadoSolicitudDTO();
        dto.setEstado(EstadoSolicitud.CANCELADA);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(solicitudRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.of(solicitud));

        assertThrows(ResponseStatusException.class, () -> gestorSolicitudService.cambiarEstado(10L, 100L, dto));
    }
}
