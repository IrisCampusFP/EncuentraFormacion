package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionDetalleDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionFiltroDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.service.formacion.FormacionPublicService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Nota: el controller ya no recibe Pageable del exterior — resuelve el Sort internamente
// a partir del parámetro sortBy semántico. Los tests usan la firma real del metodo.

@ExtendWith(MockitoExtension.class)
@DisplayName("FormacionController")
class FormacionControllerTest {

    @Mock private FormacionPublicService formacionPublicService;
    @InjectMocks private FormacionController controller;

    @Nested
    @DisplayName("GET /formaciones")
    class BuscarTests {

        @Test
        @DisplayName("devuelve 200 con página de resultados")
        void buscar_devuelve200() {
            FormacionResumenDTO dto = new FormacionResumenDTO();
            dto.setId(1L);
            Page<FormacionResumenDTO> pagina = new PageImpl<>(List.of(dto));
            when(formacionPublicService.buscar(any(), any(), any())).thenReturn(pagina);

            ResponseEntity<Page<FormacionResumenDTO>> response =
                controller.buscar(new FormacionFiltroDTO(), "recientes", 0, 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("devuelve 200 con página vacía si no hay resultados")
        void buscar_sinResultados_devuelve200Vacio() {
            when(formacionPublicService.buscar(any(), any(), any())).thenReturn(Page.empty());

            ResponseEntity<Page<FormacionResumenDTO>> response =
                controller.buscar(new FormacionFiltroDTO(), "recientes", 0, 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
        }

        @ParameterizedTest
        @DisplayName("acepta todos los sortBy semánticos sin lanzar excepción")
        @ValueSource(strings = {
            "recientes", "valoracion", "precioAsc", "precioDesc", "proximasFechas", "az", "desconocido"
        })
        void buscar_todosLosSortBy_devuelve200(String sortBy) {
            when(formacionPublicService.buscar(any(), any(), any())).thenReturn(Page.empty());

            ResponseEntity<Page<FormacionResumenDTO>> response =
                controller.buscar(new FormacionFiltroDTO(), sortBy, 0, 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("GET /formaciones/{id}")
    class DetalleTests {

        @Test
        @DisplayName("devuelve 200 con el detalle de la formación")
        void detalle_existe_devuelve200() {
            java.util.UUID uuid = java.util.UUID.randomUUID();
            FormacionDetalleDTO dto = new FormacionDetalleDTO();
            dto.setUuid(uuid);
            when(formacionPublicService.findByUuid(uuid)).thenReturn(dto);

            ResponseEntity<FormacionDetalleDTO> response = controller.detalle(uuid);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getUuid()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("propaga 404 si la formación no existe o no está activa")
        void detalle_noExiste_propaga404() {
            java.util.UUID uuid = java.util.UUID.randomUUID();
            when(formacionPublicService.findByUuid(uuid))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> controller.detalle(uuid))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }
}
