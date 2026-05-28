package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ProvinciaDTO;
import com.irisperez.tfg.encuentraformacion.service.catalogo.ProvinciaService;
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
@DisplayName("ProvinciaController")
class ProvinciaControllerTest {

    @Mock private ProvinciaService provinciaService;
    @InjectMocks private ProvinciaController controller;

    @Test
    @DisplayName("listar devuelve todas las provincias")
    void listar_ok() {
        ProvinciaDTO dto = new ProvinciaDTO();
        when(provinciaService.listar()).thenReturn(List.of(dto));

        ResponseEntity<List<ProvinciaDTO>> resp = controller.listar();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("listar devuelve lista vacía si no hay datos")
    void listar_vacio() {
        when(provinciaService.listar()).thenReturn(List.of());

        ResponseEntity<List<ProvinciaDTO>> resp = controller.listar();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }
}
