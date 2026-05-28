package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ComunidadAutonomaDTO;
import com.irisperez.tfg.encuentraformacion.service.catalogo.ComunidadAutonomaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComunidadAutonomaController")
class ComunidadAutonomaControllerTest {

    @Mock private ComunidadAutonomaService comunidadAutonomaService;
    @InjectMocks private ComunidadAutonomaController controller;

    @Test
    @DisplayName("listar devuelve todas las comunidades autónomas con 200")
    void listar_ok() {
        ComunidadAutonomaDTO dto = new ComunidadAutonomaDTO();
        when(comunidadAutonomaService.listar()).thenReturn(List.of(dto));

        ResponseEntity<List<ComunidadAutonomaDTO>> resp = controller.listar();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("listar devuelve lista vacía si no hay comunidades registradas")
    void listar_vacio() {
        when(comunidadAutonomaService.listar()).thenReturn(List.of());

        ResponseEntity<List<ComunidadAutonomaDTO>> resp = controller.listar();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }
}
