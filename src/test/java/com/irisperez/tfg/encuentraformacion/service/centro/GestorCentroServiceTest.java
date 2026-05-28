package com.irisperez.tfg.encuentraformacion.service.centro;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.EditarCentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.mapper.centro.CentroGestorMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorCentroServiceTest {

    @Mock
    private CentroRepository centroRepository;

    @Mock
    private CentroGestorMapper centroGestorMapper;

    @InjectMocks
    private GestorCentroService gestorCentroService;

    private Centro centro;
    private CentroGestorDTO centroDTO;

    @BeforeEach
    void setUp() {
        centro = new Centro();
        centro.setId(1L);

        centroDTO = new CentroGestorDTO();
        centroDTO.setId(1L);
    }

    @Test
    void getCentro_ok() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(centroGestorMapper.toDTO(centro)).thenReturn(centroDTO);

        CentroGestorDTO result = gestorCentroService.getCentro(10L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getCentro_sinCentro_lanza404() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gestorCentroService.getCentro(10L));
    }

    @Test
    void editarCentro_ok() {
        EditarCentroGestorDTO dto = new EditarCentroGestorDTO();
        dto.setDescripcion("Nueva desc");

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(centroRepository.save(any(Centro.class))).thenReturn(centro);
        when(centroGestorMapper.toDTO(any(Centro.class))).thenReturn(centroDTO);

        CentroGestorDTO result = gestorCentroService.editarCentro(10L, dto);

        assertNotNull(result);
        assertEquals("Nueva desc", centro.getDescripcion());
        verify(centroRepository).save(centro);
    }

    @Test
    void editarCentro_sinCentro_lanza404() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gestorCentroService.editarCentro(10L, new EditarCentroGestorDTO()));
    }
}
