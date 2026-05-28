package com.irisperez.tfg.encuentraformacion.service.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.TipoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.TipoEstudiosMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.TipoEstudios;
import com.irisperez.tfg.encuentraformacion.repository.TipoEstudiosRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TipoEstudiosService")
class TipoEstudiosServiceTest {

    @Mock private TipoEstudiosRepository tipoEstudiosRepository;
    @Mock private TipoEstudiosMapper tipoEstudiosMapper;
    @InjectMocks private TipoEstudiosService service;

    @Test
    @DisplayName("listar devuelve todos los tipos mapeados")
    void listar_ok() {
        TipoEstudios entidad = new TipoEstudios(); entidad.setId(1L); entidad.setNombre("FP Grado Medio");
        TipoEstudiosDTO dto = new TipoEstudiosDTO(); dto.setId(1L); dto.setNombre("FP Grado Medio");
        when(tipoEstudiosRepository.findAll()).thenReturn(List.of(entidad));
        when(tipoEstudiosMapper.toDTOList(List.of(entidad))).thenReturn(List.of(dto));

        List<TipoEstudiosDTO> result = service.listar();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("FP Grado Medio");
    }

    @Test
    @DisplayName("listar devuelve lista vacía si no hay datos")
    void listar_vacio() {
        when(tipoEstudiosRepository.findAll()).thenReturn(List.of());
        when(tipoEstudiosMapper.toDTOList(List.of())).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }
}
