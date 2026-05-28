package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.faq.CrearFaqDTO;
import com.irisperez.tfg.encuentraformacion.dto.faq.EditarFaqDTO;
import com.irisperez.tfg.encuentraformacion.dto.faq.FaqGestorDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.faq.GestorFaqService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorFaqControllerTest {

    @Mock private GestorFaqService service;
    @InjectMocks private GestorFaqController controller;

    private CustomUserDetails user(long id) {
        CustomUserDetails u = mock(CustomUserDetails.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    @Test
    void getFaqs_ok() {
        when(service.getFaqs(1L)).thenReturn(List.of(new FaqGestorDTO()));

        var resp = controller.getFaqs(user(1L));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void crear_devuelve201() {
        var dto = new CrearFaqDTO();
        var resultado = new FaqGestorDTO();
        when(service.crear(1L, dto)).thenReturn(resultado);

        var resp = controller.crear(user(1L), dto);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isSameAs(resultado);
    }

    @Test
    void editar_ok() {
        var dto = new EditarFaqDTO();
        var resultado = new FaqGestorDTO();
        when(service.editar(1L, 5L, dto)).thenReturn(resultado);

        var resp = controller.editar(user(1L), 5L, dto);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(resultado);
    }

    @Test
    void eliminar_devuelve204() {
        doNothing().when(service).eliminar(1L, 5L);

        var resp = controller.eliminar(user(1L), 5L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).eliminar(1L, 5L);
    }
}
