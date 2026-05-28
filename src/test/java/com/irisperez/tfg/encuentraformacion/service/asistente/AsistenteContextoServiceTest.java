package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.GradoEstudios;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.repository.EstudianteRepository;
import com.irisperez.tfg.encuentraformacion.repository.SolicitudFormacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsistenteContextoService")
class AsistenteContextoServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SolicitudFormacionRepository solicitudRepository;

    @InjectMocks private AsistenteContextoService service;

    private Estudiante estudiante;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Ana");
        usuario.setEmail("ana@test.com");

        GradoEstudios grado = new GradoEstudios();
        grado.setId(1L);
        grado.setNombre("Bachillerato");

        estudiante = new Estudiante();
        estudiante.setId(10L);
        estudiante.setUsuario(usuario);
        estudiante.setGradoEstudios(grado);
    }

    @Test
    @DisplayName("buildSystemPrompt con perfil completo contiene nombre y grado")
    void buildSystemPromptConPerfilCompletoContieneNombreYGrado() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findNombresFavoritosTop5(eq(10L), any(PageRequest.class)))
            .thenReturn(List.of());
        when(solicitudRepository.findTop3ByEstudianteIdOrderByFecha(eq(10L), any(PageRequest.class)))
            .thenReturn(List.of());

        String prompt = service.buildSystemPrompt(1L);

        assertThat(prompt).contains("Ana");
        assertThat(prompt).contains("Bachillerato");
    }

    @Test
    @DisplayName("buildSystemPrompt con favoritos contiene nombres de favoritas")
    void buildSystemPromptConFavoritosContieneNombresFavoritos() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findNombresFavoritosTop5(eq(10L), any(PageRequest.class)))
            .thenReturn(List.of("Grado Superior en DAM", "Bachillerato Tecnológico"));
        when(solicitudRepository.findTop3ByEstudianteIdOrderByFecha(eq(10L), any(PageRequest.class)))
            .thenReturn(List.of());

        String prompt = service.buildSystemPrompt(1L);

        assertThat(prompt).contains("Grado Superior en DAM");
        assertThat(prompt).contains("Bachillerato Tecnológico");
    }

    @Test
    @DisplayName("buildSystemPrompt sin grado de estudios no lanza excepción")
    void buildSystemPromptSinGradoEstudiosNoLanzaExcepcion() {
        estudiante.setGradoEstudios(null);
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findNombresFavoritosTop5(eq(10L), any(PageRequest.class)))
            .thenReturn(List.of());
        when(solicitudRepository.findTop3ByEstudianteIdOrderByFecha(eq(10L), any(PageRequest.class)))
            .thenReturn(List.of());

        assertThatCode(() -> service.buildSystemPrompt(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("buildSystemPrompt sin favoritas indica ninguna guardada")
    void buildSystemPromptSinFavoritosIndicaNingunaGuardada() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findNombresFavoritosTop5(eq(10L), any(PageRequest.class)))
            .thenReturn(List.of());
        when(solicitudRepository.findTop3ByEstudianteIdOrderByFecha(eq(10L), any(PageRequest.class)))
            .thenReturn(List.of());

        String prompt = service.buildSystemPrompt(1L);

        assertThat(prompt).containsIgnoringCase("ninguna");
    }

    @Test
    @DisplayName("buildSystemPrompt con provincia y localidad muestra 'Localidad (Provincia)'")
    void buildSystemPromptConProvinciaYLocalidad_muestraUbicacionCompleta() {
        Provincia provincia = new Provincia();
        provincia.setId(28L);
        provincia.setNombre("Madrid");
        provincia.setCodigoIne("28");
        estudiante.setProvincia(provincia);
        estudiante.setLocalidad("Alcorcón");

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findNombresFavoritosTop5(eq(10L), any(PageRequest.class))).thenReturn(List.of());
        when(solicitudRepository.findTop3ByEstudianteIdOrderByFecha(eq(10L), any(PageRequest.class))).thenReturn(List.of());

        String prompt = service.buildSystemPrompt(1L);

        assertThat(prompt).contains("Alcorcón (Madrid)");
    }

    @Test
    @DisplayName("buildSystemPrompt con solo provincia muestra el nombre de la provincia")
    void buildSystemPromptConSoloProvincia_muestraProvincia() {
        Provincia provincia = new Provincia();
        provincia.setId(41L);
        provincia.setNombre("Sevilla");
        provincia.setCodigoIne("41");
        estudiante.setProvincia(provincia);

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findNombresFavoritosTop5(eq(10L), any(PageRequest.class))).thenReturn(List.of());
        when(solicitudRepository.findTop3ByEstudianteIdOrderByFecha(eq(10L), any(PageRequest.class))).thenReturn(List.of());

        String prompt = service.buildSystemPrompt(1L);

        assertThat(prompt).contains("Sevilla");
        assertThat(prompt).doesNotContain("no especificada");
    }

    @Test
    @DisplayName("buildSystemPrompt sin ubicación indica que no está especificada")
    void buildSystemPromptSinUbicacion_indicaNoEspecificada() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findNombresFavoritosTop5(eq(10L), any(PageRequest.class))).thenReturn(List.of());
        when(solicitudRepository.findTop3ByEstudianteIdOrderByFecha(eq(10L), any(PageRequest.class))).thenReturn(List.of());

        String prompt = service.buildSystemPrompt(1L);

        assertThat(prompt).contains("no especificada");
    }

    // ── Cobertura del switch mapearAccesibles() ──────────────────────────────

    private String buildPromptConGrado(String gradoNombre) {
        GradoEstudios grado = new GradoEstudios();
        grado.setId(99L);
        grado.setNombre(gradoNombre);
        estudiante.setGradoEstudios(grado);

        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findNombresFavoritosTop5(eq(10L), any(PageRequest.class))).thenReturn(List.of());
        when(solicitudRepository.findTop3ByEstudianteIdOrderByFecha(eq(10L), any(PageRequest.class))).thenReturn(List.of());

        return service.buildSystemPrompt(1L);
    }

    @Test
    @DisplayName("mapearAccesibles: Sin estudios menciona formación no reglada")
    void mapearAccesibles_sinEstudios() {
        assertThat(buildPromptConGrado("Sin estudios")).containsIgnoringCase("reglada");
    }

    @Test
    @DisplayName("mapearAccesibles: Primaria menciona formación no reglada")
    void mapearAccesibles_primaria() {
        assertThat(buildPromptConGrado("Primaria")).containsIgnoringCase("reglada");
    }

    @Test
    @DisplayName("mapearAccesibles: ESO menciona FP Grado Medio")
    void mapearAccesibles_eso() {
        assertThat(buildPromptConGrado("ESO")).contains("FP Grado Medio");
    }

    @Test
    @DisplayName("mapearAccesibles: FP Básica menciona FP Grado Superior")
    void mapearAccesibles_fpBasica() {
        assertThat(buildPromptConGrado("FP Básica")).contains("FP Grado Superior");
    }

    @Test
    @DisplayName("mapearAccesibles: Grado Medio menciona FP Grado Superior")
    void mapearAccesibles_gradoMedio() {
        assertThat(buildPromptConGrado("Grado Medio")).contains("FP Grado Superior");
    }

    @Test
    @DisplayName("mapearAccesibles: Grado Superior menciona Grado Universitario")
    void mapearAccesibles_gradoSuperior() {
        assertThat(buildPromptConGrado("Grado Superior")).contains("Grado Universitario");
    }

    @Test
    @DisplayName("mapearAccesibles: Grado Universitario menciona Máster Oficial")
    void mapearAccesibles_gradoUniversitario() {
        assertThat(buildPromptConGrado("Grado Universitario")).contains("Máster Oficial");
    }

    @Test
    @DisplayName("mapearAccesibles: Máster Oficial menciona Doctorado")
    void mapearAccesibles_masterOficial() {
        assertThat(buildPromptConGrado("Máster Oficial")).contains("Doctorado");
    }

    @Test
    @DisplayName("mapearAccesibles: Doctorado menciona cualquier formación")
    void mapearAccesibles_doctorado() {
        assertThat(buildPromptConGrado("Doctorado")).isNotBlank();
    }

    @Test
    @DisplayName("mapearAccesibles: grado desconocido cae al default")
    void mapearAccesibles_default() {
        assertThat(buildPromptConGrado("Otro desconocido")).containsIgnoringCase("no especificado");
    }
}
