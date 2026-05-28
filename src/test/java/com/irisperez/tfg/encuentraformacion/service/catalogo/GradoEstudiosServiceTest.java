package com.irisperez.tfg.encuentraformacion.service.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.GradoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.GradoEstudiosMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.GradoEstudios;
import com.irisperez.tfg.encuentraformacion.repository.GradoEstudiosRepository;
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
@DisplayName("GradoEstudiosService")
class GradoEstudiosServiceTest {

    @Mock private GradoEstudiosRepository gradoEstudiosRepository;
    @Mock private GradoEstudiosMapper gradoEstudiosMapper;
    @InjectMocks private GradoEstudiosService service;

    @Test
    @DisplayName("listar devuelve todos los grados mapeados")
    void listar_ok() {
        GradoEstudios entidad = new GradoEstudios(); entidad.setId(1L); entidad.setNombre("Bachillerato");
        GradoEstudiosDTO dto = new GradoEstudiosDTO(); dto.setId(1L); dto.setNombre("Bachillerato");
        when(gradoEstudiosRepository.findAll()).thenReturn(List.of(entidad));
        when(gradoEstudiosMapper.toDTOList(List.of(entidad))).thenReturn(List.of(dto));

        List<GradoEstudiosDTO> result = service.listar();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Bachillerato");
    }

    @Test
    @DisplayName("listar devuelve lista vacía si no hay grados")
    void listar_vacio() {
        when(gradoEstudiosRepository.findAll()).thenReturn(List.of());
        when(gradoEstudiosMapper.toDTOList(List.of())).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }
}
