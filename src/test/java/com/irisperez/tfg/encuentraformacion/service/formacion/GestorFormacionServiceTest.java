package com.irisperez.tfg.encuentraformacion.service.formacion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.CrearFormacionDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.EditarFormacionDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.mapper.formacion.FormacionGestorMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.TipoEstudios;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.FormacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.TipoEstudiosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorFormacionServiceTest {

    @Mock
    private FormacionRepository formacionRepository;
    @Mock
    private CentroRepository centroRepository;
    @Mock
    private TipoEstudiosRepository tipoEstudiosRepository;
    @Mock
    private com.irisperez.tfg.encuentraformacion.repository.ConversacionRepository conversacionRepository;
    @Mock
    private FormacionGestorMapper formacionGestorMapper;

    @InjectMocks
    private GestorFormacionService gestorFormacionService;

    private Centro centro;

    @BeforeEach
    void setUp() {
        centro = new Centro();
        centro.setId(1L);
    }

    @Test
    void getFormaciones_ok() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        Formacion f = new Formacion();
        f.setId(100L);
        Page<Formacion> page = new PageImpl<>(List.of(f));
        when(formacionRepository.findByCentroId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(formacionGestorMapper.toDTO(f)).thenReturn(new FormacionGestorDTO());
        when(formacionRepository.countSolicitudesPendientesByFormacionId(100L)).thenReturn(5L);
        when(conversacionRepository.countByFormacionId(100L)).thenReturn(2L);

        Page<FormacionGestorDTO> result = gestorFormacionService.getFormaciones(10L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(5L, result.getContent().get(0).getSolicitudesPendientes());
        assertEquals(2L, result.getContent().get(0).getChatsActivos());
    }

    @Test
    void crear_ok() {
        CrearFormacionDTO dto = new CrearFormacionDTO();
        dto.setTipoEstudiosId(2L);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(tipoEstudiosRepository.findById(2L)).thenReturn(Optional.of(new TipoEstudios()));
        when(formacionRepository.save(any(Formacion.class))).thenReturn(new Formacion());
        when(formacionGestorMapper.toDTO(any())).thenReturn(new FormacionGestorDTO());

        FormacionGestorDTO result = gestorFormacionService.crear(10L, dto);

        assertNotNull(result);
        assertEquals(0L, result.getSolicitudesPendientes());
    }

    @Test
    void crear_tipoEstudiosInvalido_lanza400() {
        CrearFormacionDTO dto = new CrearFormacionDTO();
        dto.setTipoEstudiosId(2L);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(tipoEstudiosRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gestorFormacionService.crear(10L, dto));
    }

    @Test
    void editar_ok() {
        EditarFormacionDTO dto = new EditarFormacionDTO();
        dto.setNombre("Nuevo");

        Formacion f = new Formacion();
        f.setId(100L);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(formacionRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.of(f));
        when(formacionRepository.save(f)).thenReturn(f);
        when(formacionGestorMapper.toDTO(f)).thenReturn(new FormacionGestorDTO());

        gestorFormacionService.editar(10L, 100L, dto);

        assertEquals("Nuevo", f.getNombre());
    }

    @Test
    void editar_formacionDeOtroCentro_lanza404() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(formacionRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gestorFormacionService.editar(10L, 100L, new EditarFormacionDTO()));
    }

    @Test
    void desactivar_ok() {
        Formacion f = new Formacion();
        f.setId(100L);
        f.setActiva(true);

        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(formacionRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.of(f));

        gestorFormacionService.desactivar(10L, 100L);

        assertFalse(f.getActiva());
        verify(formacionRepository).save(f);
    }

    @Test
    void desactivar_formacionDeOtroCentro_lanza404() {
        when(centroRepository.findByGestorId(10L)).thenReturn(Optional.of(centro));
        when(formacionRepository.findByIdAndCentroId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gestorFormacionService.desactivar(10L, 100L));
    }
}
