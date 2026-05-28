package com.irisperez.tfg.encuentraformacion.service.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ProvinciaDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.ProvinciaMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.repository.ProvinciaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProvinciaService")
class ProvinciaServiceTest {

    @Mock private ProvinciaRepository provinciaRepository;
    @Mock private ProvinciaMapper provinciaMapper;
    @InjectMocks private ProvinciaService service;

    @Nested
    @DisplayName("listar()")
    class ListarTests {
        @Test
        @DisplayName("devuelve todas las provincias mapeadas")
        void listar_ok() {
            Provincia entidad = new Provincia(); entidad.setId(1L); entidad.setNombre("Madrid");
            ProvinciaDTO dto = new ProvinciaDTO(); dto.setId(1L); dto.setNombre("Madrid");
            when(provinciaRepository.findAll(any(Sort.class))).thenReturn(List.of(entidad));
            when(provinciaMapper.toDTOList(List.of(entidad))).thenReturn(List.of(dto));

            List<ProvinciaDTO> result = service.listar();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNombre()).isEqualTo("Madrid");
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay provincias")
        void listar_vacio() {
            when(provinciaRepository.findAll(any(Sort.class))).thenReturn(List.of());
            when(provinciaMapper.toDTOList(List.of())).thenReturn(List.of());

            assertThat(service.listar()).isEmpty();
        }
    }

    @Nested
    @DisplayName("buscarPorNombre()")
    class BuscarTests {
        @Test
        @DisplayName("encuentra provincia existente")
        void buscarPorNombre_ok() {
            Provincia p = new Provincia(); p.setId(1L); p.setNombre("Sevilla");
            when(provinciaRepository.findByNombreIgnoreCase("Sevilla")).thenReturn(Optional.of(p));

            Provincia result = service.buscarPorNombre("Sevilla");

            assertThat(result.getNombre()).isEqualTo("Sevilla");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si no existe")
        void buscarPorNombre_noExiste() {
            when(provinciaRepository.findByNombreIgnoreCase("Inexistente")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorNombre("Inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inexistente");
        }
    }
}
