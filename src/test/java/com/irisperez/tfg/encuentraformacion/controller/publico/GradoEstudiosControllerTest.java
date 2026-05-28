package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.GradoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.service.catalogo.GradoEstudiosService;
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
@DisplayName("GradoEstudiosController")
class GradoEstudiosControllerTest {

    @Mock private GradoEstudiosService gradoEstudiosService;
    @InjectMocks private GradoEstudiosController controller;

    @Test
    @DisplayName("listar devuelve todos los grados")
    void listar_ok() {
        GradoEstudiosDTO dto = new GradoEstudiosDTO();
        when(gradoEstudiosService.listar()).thenReturn(List.of(dto));

        ResponseEntity<List<GradoEstudiosDTO>> resp = controller.listar();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("listar devuelve lista vacía si no hay datos")
    void listar_vacio() {
        when(gradoEstudiosService.listar()).thenReturn(List.of());

        ResponseEntity<List<GradoEstudiosDTO>> resp = controller.listar();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }
}
