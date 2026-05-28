package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.TipoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.service.catalogo.TipoEstudiosService;
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
@DisplayName("TipoEstudiosController")
class TipoEstudiosControllerTest {

    @Mock private TipoEstudiosService tipoEstudiosService;
    @InjectMocks private TipoEstudiosController controller;

    @Test
    @DisplayName("listar devuelve todos los tipos de estudios")
    void listar_ok() {
        TipoEstudiosDTO dto = new TipoEstudiosDTO();
        dto.setId(7L);
        dto.setNombre("FP Grado Medio");
        when(tipoEstudiosService.listar()).thenReturn(List.of(dto));

        ResponseEntity<List<TipoEstudiosDTO>> resp = controller.listar();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getNombre()).isEqualTo("FP Grado Medio");
    }

    @Test
    @DisplayName("listar devuelve lista vacía si no hay datos")
    void listar_vacio() {
        when(tipoEstudiosService.listar()).thenReturn(List.of());

        ResponseEntity<List<TipoEstudiosDTO>> resp = controller.listar();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }
}
