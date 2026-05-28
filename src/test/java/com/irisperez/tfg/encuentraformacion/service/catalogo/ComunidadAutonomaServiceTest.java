package com.irisperez.tfg.encuentraformacion.service.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ComunidadAutonomaDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.ComunidadAutonomaMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import com.irisperez.tfg.encuentraformacion.repository.ComunidadAutonomaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComunidadAutonomaServiceTest {

    @Mock private ComunidadAutonomaRepository repo;
    @Mock private ComunidadAutonomaMapper mapper;
    @InjectMocks private ComunidadAutonomaService service;

    @Test
    void listar_devuelveDTOs() {
        var entidad = new ComunidadAutonoma();
        var dto = new ComunidadAutonomaDTO();
        when(repo.findAll(Sort.by("nombre"))).thenReturn(List.of(entidad));
        when(mapper.toDTOList(List.of(entidad))).thenReturn(List.of(dto));

        List<ComunidadAutonomaDTO> result = service.listar();

        assertThat(result).hasSize(1);
    }

    @Test
    void listar_vacio() {
        when(repo.findAll(Sort.by("nombre"))).thenReturn(List.of());
        when(mapper.toDTOList(List.of())).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }
}
