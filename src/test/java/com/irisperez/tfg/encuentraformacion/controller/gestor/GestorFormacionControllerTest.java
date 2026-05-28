package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.formacion.CrearFormacionDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.EditarFormacionDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.formacion.GestorFormacionService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorFormacionControllerTest {

    @Mock private GestorFormacionService service;
    @InjectMocks private GestorFormacionController controller;

    private CustomUserDetails user(long id) {
        CustomUserDetails u = mock(CustomUserDetails.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    @Test
    void getFormaciones_ok() {
        var page = new PageImpl<>(List.of(new FormacionGestorDTO()));
        when(service.getFormaciones(eq(1L), any(Pageable.class))).thenReturn(page);

        var resp = controller.getFormaciones(user(1L), Pageable.unpaged());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
    }

    @Test
    void crear_devuelve201() {
        var dto = new CrearFormacionDTO();
        var resultado = new FormacionGestorDTO();
        when(service.crear(1L, dto)).thenReturn(resultado);

        var resp = controller.crear(user(1L), dto);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isSameAs(resultado);
    }

    @Test
    void editar_ok() {
        var dto = new EditarFormacionDTO();
        var resultado = new FormacionGestorDTO();
        when(service.editar(1L, 10L, dto)).thenReturn(resultado);

        var resp = controller.editar(user(1L), 10L, dto);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(resultado);
    }

    @Test
    void desactivar_devuelve204() {
        doNothing().when(service).desactivar(1L, 10L);

        var resp = controller.desactivar(user(1L), 10L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).desactivar(1L, 10L);
    }
}
