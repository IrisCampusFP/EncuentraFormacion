package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroBuscadorDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroPerfilDTO;
import com.irisperez.tfg.encuentraformacion.service.centro.CentroPublicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CentroPublicController")
class CentroPublicControllerTest {

    @Mock private CentroPublicService centroPublicService;
    @InjectMocks private CentroPublicController controller;

    @Nested
    @DisplayName("buscar()")
    class BuscarTests {
        @Test
        @DisplayName("devuelve página de centros con parámetros por defecto")
        void buscar_sinFiltros() {
            Page<CentroBuscadorDTO> page = new PageImpl<>(List.of(new CentroBuscadorDTO()));
            when(centroPublicService.buscarCentros(any(), any(), any(), any(), eq("az"), any())).thenReturn(page);

            ResponseEntity<Page<CentroBuscadorDTO>> resp =
                controller.buscar(null, null, null, null, 0, 10, "az");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("acepta sortBy=za")
        void buscar_sortZa() {
            Page<CentroBuscadorDTO> page = new PageImpl<>(List.of());
            when(centroPublicService.buscarCentros(any(), any(), any(), any(), eq("za"), any())).thenReturn(page);

            ResponseEntity<Page<CentroBuscadorDTO>> resp =
                controller.buscar(null, null, null, null, 0, 10, "za");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("acepta sortBy=valorados")
        void buscar_sortValorados() {
            Page<CentroBuscadorDTO> page = new PageImpl<>(List.of());
            when(centroPublicService.buscarCentros(any(), any(), any(), any(), eq("valorados"), any())).thenReturn(page);

            ResponseEntity<Page<CentroBuscadorDTO>> resp =
                controller.buscar(null, null, null, null, 0, 10, "valorados");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("getPerfil()")
    class GetPerfilTests {
        @Test
        @DisplayName("devuelve perfil por uuid")
        void getPerfil_ok() {
            UUID uuid = UUID.randomUUID();
            CentroPerfilDTO dto = new CentroPerfilDTO();
            when(centroPublicService.getPerfilByUuid(uuid)).thenReturn(dto);

            ResponseEntity<CentroPerfilDTO> resp = controller.getPerfil(uuid);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isEqualTo(dto);
        }
    }
}
