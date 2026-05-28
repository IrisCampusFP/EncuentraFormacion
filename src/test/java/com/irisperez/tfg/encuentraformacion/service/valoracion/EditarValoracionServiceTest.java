package com.irisperez.tfg.encuentraformacion.service.valoracion;

import com.irisperez.tfg.encuentraformacion.dto.valoracion.EditarValoracionDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.ValoracionDTO;
import com.irisperez.tfg.encuentraformacion.mapper.valoracion.ValoracionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.Valoracion;
import com.irisperez.tfg.encuentraformacion.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValoracionEstudianteService — editar")
class EditarValoracionServiceTest {

    @Mock private ValoracionRepository valoracionRepository;
    @Mock private FormacionRepository formacionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ValoracionMapper valoracionMapper;

    @InjectMocks private ValoracionEstudianteService service;

    private Estudiante est;
    private Valoracion valoracion;
    private EditarValoracionDTO dto;

    @BeforeEach
    void setUp() {
        est = new Estudiante();
        est.setId(1L);

        Estudiante otroEst = new Estudiante();
        otroEst.setId(99L);

        valoracion = new Valoracion();
        valoracion.setEstudiante(est);
        valoracion.setEstrellas(3);

        dto = new EditarValoracionDTO();
        dto.setEstrellas(5);
        dto.setComentario("Actualizado");
    }

    @Test
    @DisplayName("edita valoración propia correctamente")
    void editar_ok() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(est));
        when(valoracionRepository.findById(10L)).thenReturn(Optional.of(valoracion));
        when(valoracionRepository.save(any())).thenReturn(valoracion);
        ValoracionDTO expected = new ValoracionDTO();
        when(valoracionMapper.toDTO(valoracion)).thenReturn(expected);

        assertThat(service.editar(10L, dto, 1L)).isEqualTo(expected);
        assertThat(valoracion.getEstrellas()).isEqualTo(5);
        assertThat(valoracion.getComentario()).isEqualTo("Actualizado");
        assertThat(valoracion.getFechaModificacion()).isNotNull();
    }

    @Test
    @DisplayName("404 si la valoración no existe")
    void editar_noExiste_lanza404() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(est));
        when(valoracionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(99L, dto, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("403 si el estudiante intenta editar la valoración de otro")
    void editar_ajena_lanza403() {
        Estudiante otro = new Estudiante();
        otro.setId(99L);
        valoracion.setEstudiante(otro);

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(est));
        when(valoracionRepository.findById(10L)).thenReturn(Optional.of(valoracion));

        assertThatThrownBy(() -> service.editar(10L, dto, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
