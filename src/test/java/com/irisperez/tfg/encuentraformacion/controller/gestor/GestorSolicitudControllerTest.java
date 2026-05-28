package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.CambiarEstadoSolicitudDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestorDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.solicitud.formacion.GestorSolicitudService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorSolicitudControllerTest {

    @Mock private GestorSolicitudService service;
    @InjectMocks private GestorSolicitudController controller;

    private CustomUserDetails user(long id) {
        CustomUserDetails u = mock(CustomUserDetails.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    @Test
    void getSolicitudes_ok() {
        var page = new PageImpl<>(List.of(new SolicitudGestorDTO()));
        when(service.getSolicitudes(eq(1L), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(page);

        var resp = controller.getSolicitudes(user(1L), null, null, null, Pageable.unpaged());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
    }

    @Test
    void getById_ok() {
        var dto = new SolicitudGestorDTO();
        when(service.getById(1L, 10L)).thenReturn(dto);

        var resp = controller.getById(user(1L), 10L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void countPendientes_ok() {
        when(service.countPendientes(1L)).thenReturn(3L);

        var resp = controller.countPendientes(user(1L));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(3L);
    }

    @Test
    void countByEstado_ok() {
        when(service.countByEstadoWithFilters(1L, EstadoSolicitud.PENDIENTE, null, null)).thenReturn(5L);

        var resp = controller.countByEstado(user(1L), EstadoSolicitud.PENDIENTE, null, null);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(5L);
    }

    @Test
    void cambiarEstado_ok() {
        var dto = new CambiarEstadoSolicitudDTO();
        var resultado = new SolicitudGestorDTO();
        when(service.cambiarEstado(1L, 10L, dto)).thenReturn(resultado);

        var resp = controller.cambiarEstado(user(1L), 10L, dto);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(resultado);
    }
}
