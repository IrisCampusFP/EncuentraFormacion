package com.irisperez.tfg.encuentraformacion.service.valoracion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.CrearValoracionDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.ValoracionDTO;
import com.irisperez.tfg.encuentraformacion.mapper.valoracion.ValoracionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Valoracion;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.repository.projection.ValoracionStatsProjection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValoracionEstudianteService")
class ValoracionEstudianteServiceTest {

    @Mock private ValoracionRepository valoracionRepository;
    @Mock private FormacionRepository formacionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ValoracionMapper valoracionMapper;

    @InjectMocks private ValoracionEstudianteService service;

    @Test
    @DisplayName("crea valoración correctamente")
    void crear_ok() {
        UUID uuid = UUID.randomUUID();
        Estudiante est = new Estudiante(); est.setId(1L);
        Formacion f = new Formacion(); f.setId(10L); f.setActiva(true);
        Centro c = new Centro(); f.setCentro(c);

        CrearValoracionDTO dto = new CrearValoracionDTO();
        dto.setFormacionUuid(uuid); dto.setEstrellas(5); dto.setComentario("Muy buena");

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(est));
        when(formacionRepository.findByUuid(uuid)).thenReturn(Optional.of(f));
        when(valoracionRepository.existsByEstudianteIdAndFormacionId(1L, 10L)).thenReturn(false);
        Valoracion saved = new Valoracion();
        when(valoracionRepository.save(any())).thenReturn(saved);
        ValoracionDTO expected = new ValoracionDTO();
        when(valoracionMapper.toDTO(saved)).thenReturn(expected);

        assertThat(service.crear(dto, 1L)).isEqualTo(expected);
    }

    @Test
    @DisplayName("409 si ya valoró esa formación")
    void crear_duplicada_lanza409() {
        UUID uuid = UUID.randomUUID();
        Estudiante est = new Estudiante(); est.setId(1L);
        Formacion f = new Formacion(); f.setId(10L); f.setActiva(true);
        Centro c = new Centro(); f.setCentro(c);
        CrearValoracionDTO dto = new CrearValoracionDTO();
        dto.setFormacionUuid(uuid); dto.setEstrellas(4);

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(est));
        when(formacionRepository.findByUuid(uuid)).thenReturn(Optional.of(f));
        when(valoracionRepository.existsByEstudianteIdAndFormacionId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(dto, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("404 si el perfil de estudiante no existe al crear")
    void crear_sinPerfilEstudiante_lanza404() {
        CrearValoracionDTO dto = new CrearValoracionDTO();
        dto.setFormacionUuid(UUID.randomUUID());
        when(estudianteRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(dto, 99L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("cargarValoraciones: enriquece los DTOs con media y total de valoraciones")
    void cargarValoraciones_enriqueceDTOs() {
        FormacionResumenDTO dto1 = new FormacionResumenDTO();
        dto1.setId(10L);
        FormacionResumenDTO dto2 = new FormacionResumenDTO();
        dto2.setId(20L);

        ValoracionStatsProjection stats = mock(ValoracionStatsProjection.class);
        when(stats.getFormacionId()).thenReturn(10L);
        when(stats.getMedia()).thenReturn(4.5);
        when(stats.getTotal()).thenReturn(12L);

        when(valoracionRepository.findStatsByFormacionIds(List.of(10L, 20L))).thenReturn(List.of(stats));

        service.cargarValoraciones(List.of(dto1, dto2));

        assertThat(dto1.getValoracionMedia()).isEqualTo(4.5);
        assertThat(dto1.getTotalValoraciones()).isEqualTo(12L);
        assertThat(dto2.getValoracionMedia()).isNull();
        assertThat(dto2.getTotalValoraciones()).isNull();
    }

    @Test
    @DisplayName("cargarValoraciones: lista vacía no invoca el repositorio")
    void cargarValoraciones_listaVacia_noConsultaRepo() {
        service.cargarValoraciones(new ArrayList<>());
        verifyNoInteractions(valoracionRepository);
    }

    @Test
    @DisplayName("miValoracion: retorna DTO si el estudiante ya valoró la formación")
    void miValoracion_existente_retornaDTO() {
        UUID uuid = UUID.randomUUID();
        Estudiante est = new Estudiante(); est.setId(1L);
        Valoracion v = new Valoracion();
        ValoracionDTO expected = new ValoracionDTO();

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(est));
        when(valoracionRepository.findByEstudianteIdAndFormacionUuid(1L, uuid)).thenReturn(Optional.of(v));
        when(valoracionMapper.toDTO(v)).thenReturn(expected);

        assertThat(service.miValoracion(uuid, 1L)).isEqualTo(expected);
    }

    @Test
    @DisplayName("miValoracion: retorna null si no existe valoración propia")
    void miValoracion_inexistente_retornaNull() {
        UUID uuid = UUID.randomUUID();
        Estudiante est = new Estudiante(); est.setId(1L);

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(est));
        when(valoracionRepository.findByEstudianteIdAndFormacionUuid(1L, uuid)).thenReturn(Optional.empty());

        assertThat(service.miValoracion(uuid, 1L)).isNull();
    }

    @Test
    @DisplayName("miValoracion: 404 si el perfil de estudiante no existe")
    void miValoracion_sinPerfil_lanza404() {
        when(estudianteRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.miValoracion(UUID.randomUUID(), 99L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
