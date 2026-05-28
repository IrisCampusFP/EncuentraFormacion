package com.irisperez.tfg.encuentraformacion.service.formacion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.mapper.formacion.FormacionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.FormacionFavorita;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoritoService")
class FavoritoServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private FormacionRepository formacionRepository;
    @Mock private FormacionFavoritaRepository favoritoRepository;
    @Mock private FormacionMapper formacionMapper;
    @Mock private ValoracionEstudianteService valoracionService;

    @InjectMocks private FavoritoService service;

    private Estudiante estudiante;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante(); estudiante.setId(1L);
        Usuario u = new Usuario(); u.setId(10L); estudiante.setUsuario(u);
        pageable = PageRequest.of(0, 12);
    }

    @Test
    @DisplayName("getMisFavoritos devuelve página mapeada")
    void getMisFavoritos_ok() {
        Formacion f = new Formacion(); f.setId(5L);
        FormacionFavorita ff = new FormacionFavorita(); ff.setFormacion(f);
        FormacionResumenDTO dto = new FormacionResumenDTO(); dto.setId(5L);
        Page<FormacionFavorita> page = new PageImpl<>(List.of(ff), pageable, 1);

        when(estudianteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(estudiante));
        when(favoritoRepository.findByEstudianteId(1L, pageable)).thenReturn(page);
        when(formacionMapper.toResumenDTO(f)).thenReturn(dto);

        Page<FormacionResumenDTO> result = service.getMisFavoritos(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getMisFavoritos devuelve página vacía cuando no hay favoritas")
    void getMisFavoritos_vacio() {
        when(estudianteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(estudiante));
        when(favoritoRepository.findByEstudianteId(1L, pageable)).thenReturn(Page.empty(pageable));

        assertThat(service.getMisFavoritos(10L, pageable).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("agregar: 409 si ya es favorita")
    void agregar_yaFavorita_lanza409() {
        UUID uuid = UUID.randomUUID();
        when(estudianteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(estudiante));
        when(favoritoRepository.existsByEstudianteIdAndFormacionUuid(1L, uuid)).thenReturn(true);

        assertThatThrownBy(() -> service.agregar(uuid, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("agregar: agrega correctamente cuando no es favorita")
    void agregar_nueva_ok() {
        UUID uuid = UUID.randomUUID();
        Formacion f = new Formacion(); f.setId(20L); f.setUuid(uuid);

        when(estudianteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(estudiante));
        when(favoritoRepository.existsByEstudianteIdAndFormacionUuid(1L, uuid)).thenReturn(false);
        when(formacionRepository.findByUuid(uuid)).thenReturn(Optional.of(f));
        when(favoritoRepository.save(any(FormacionFavorita.class))).thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> service.agregar(uuid, 10L)).doesNotThrowAnyException();
        verify(favoritoRepository).save(any(FormacionFavorita.class));
    }

    @Test
    @DisplayName("agregar: 404 si la formación no existe")
    void agregar_formacionNoExiste_lanza404() {
        UUID uuid = UUID.randomUUID();
        when(estudianteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(estudiante));
        when(favoritoRepository.existsByEstudianteIdAndFormacionUuid(1L, uuid)).thenReturn(false);
        when(formacionRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agregar(uuid, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("esGuardada: devuelve true si está en favoritos")
    void esGuardada_true() {
        UUID uuid = UUID.randomUUID();
        when(estudianteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(estudiante));
        when(favoritoRepository.existsByEstudianteIdAndFormacionUuid(1L, uuid)).thenReturn(true);

        assertThat(service.esGuardada(uuid, 10L)).isTrue();
    }

    @Test
    @DisplayName("esGuardada: devuelve false si no está en favoritos")
    void esGuardada_false() {
        UUID uuid = UUID.randomUUID();
        when(estudianteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(estudiante));
        when(favoritoRepository.existsByEstudianteIdAndFormacionUuid(1L, uuid)).thenReturn(false);

        assertThat(service.esGuardada(uuid, 10L)).isFalse();
    }

    @Test
    @DisplayName("quitar: llama al delete sin excepción")
    void quitar_ok() {
        UUID uuid = UUID.randomUUID();
        when(estudianteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(estudiante));
        doNothing().when(favoritoRepository).deleteByEstudianteIdAndFormacionUuid(1L, uuid);

        assertThatCode(() -> service.quitar(uuid, 10L)).doesNotThrowAnyException();
        verify(favoritoRepository).deleteByEstudianteIdAndFormacionUuid(1L, uuid);
    }
}
