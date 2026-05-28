package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.EditarCentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.centro.GestorCentroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestorCentroControllerTest {

    @Mock private GestorCentroService service;
    @InjectMocks private GestorCentroController controller;

    private CustomUserDetails user(long id) {
        CustomUserDetails u = mock(CustomUserDetails.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    @Test
    void getCentro_ok() {
        var dto = new CentroGestorDTO();
        when(service.getCentro(1L)).thenReturn(dto);

        var resp = controller.getCentro(user(1L));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void editarCentro_ok() {
        var dto = new EditarCentroGestorDTO();
        var resultado = new CentroGestorDTO();
        when(service.editarCentro(1L, dto)).thenReturn(resultado);

        var resp = controller.editarCentro(user(1L), dto);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(resultado);
    }
}
