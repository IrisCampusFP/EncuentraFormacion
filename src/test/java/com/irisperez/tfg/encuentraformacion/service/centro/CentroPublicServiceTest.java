package com.irisperez.tfg.encuentraformacion.service.centro;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroBuscadorDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroPerfilDTO;
import com.irisperez.tfg.encuentraformacion.mapper.faq.FaqCentroMapper;
import com.irisperez.tfg.encuentraformacion.mapper.formacion.FormacionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.repository.projection.CentroValoracionStatsProjection;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CentroPublicService")
class CentroPublicServiceTest {

    @Mock private CentroRepository centroRepository;
    @Mock private FormacionRepository formacionRepository;
    @Mock private ValoracionRepository valoracionRepository;
    @Mock private FaqCentroRepository faqCentroRepository;
    @Mock private FormacionMapper formacionMapper;
    @Mock private FaqCentroMapper faqCentroMapper;
    @Mock private ValoracionEstudianteService valoracionService;

    @InjectMocks private CentroPublicService service;

    private Centro centro;

    @BeforeEach
    void setUp() {
        centro = new Centro();
        centro.setId(1L);
        centro.setUuid(java.util.UUID.randomUUID());
        centro.setNombreComercial("Academia Test");
        centro.setLocalidad("Madrid");
        centro.setProvincia(new Provincia(28L, "Madrid", "28", new ComunidadAutonoma(13L, "Madrid", "13")));
        centro.setTipo(TipoCentro.PRIVADO);
        centro.setVerificado(true);
    }

    @Nested
    @DisplayName("buscarCentros()")
    class BuscarCentrosTests {

        @Test
        @DisplayName("busca con sortBy=az usando Specification")
        void buscar_sortAz() {
            Pageable pageable = PageRequest.of(0, 10);
            Centro c = buildCentroCompleto();
            Page<Centro> page = new PageImpl<>(List.of(c));

            when(centroRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(valoracionRepository.findStatsByCentroIds(any())).thenReturn(List.of());

            Page<CentroBuscadorDTO> result = service.buscarCentros(null, null, null, null, "az", pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getNombreComercial()).isEqualTo("Academia Test");
        }

        @Test
        @DisplayName("busca con sortBy=za usando Specification")
        void buscar_sortZa() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Centro> page = new PageImpl<>(List.of());
            when(centroRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(valoracionRepository.findStatsByCentroIds(any())).thenReturn(List.of());

            Page<CentroBuscadorDTO> result = service.buscarCentros(null, null, null, null, "za", pageable);

            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("busca con sortBy=valorados usa query especial")
        void buscar_sortValorados() {
            Pageable pageable = PageRequest.of(0, 10);
            Centro c = buildCentroCompleto();
            Page<Centro> page = new PageImpl<>(List.of(c));

            when(centroRepository.findVerificadosOrderByValoracion(
                anyBoolean(), anyString(), anyBoolean(), anyString(), any(), any(), eq(pageable)
            )).thenReturn(page);
            when(valoracionRepository.findStatsByCentroIds(any())).thenReturn(List.of());

            Page<CentroBuscadorDTO> result = service.buscarCentros("test", "Madrid", null, null, "valorados", pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("mapearDTO enriquece con stats cuando existen")
        void buscar_conStats() {
            Pageable pageable = PageRequest.of(0, 10);
            Centro c = buildCentroCompleto();
            Page<Centro> page = new PageImpl<>(List.of(c));

            CentroValoracionStatsProjection stats = mock(CentroValoracionStatsProjection.class);
            when(stats.getCentroId()).thenReturn(1L);
            when(stats.getMedia()).thenReturn(4.0);
            when(stats.getTotal()).thenReturn(5L);

            when(centroRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(valoracionRepository.findStatsByCentroIds(List.of(1L))).thenReturn(List.of(stats));

            Page<CentroBuscadorDTO> result = service.buscarCentros(null, null, null, null, "az", pageable);

            assertThat(result.getContent().get(0).getValoracionMedia()).isEqualTo(4.0);
            assertThat(result.getContent().get(0).getTotalValoraciones()).isEqualTo(5);
        }

        @Test
        @DisplayName("mapearDTO con provincia null no lanza excepción")
        void buscar_centroProvincia_null() {
            Pageable pageable = PageRequest.of(0, 10);
            Centro c = buildCentroCompleto();
            c.setProvincia(null);
            Page<Centro> page = new PageImpl<>(List.of(c));

            when(centroRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(valoracionRepository.findStatsByCentroIds(any())).thenReturn(List.of());

            Page<CentroBuscadorDTO> result = service.buscarCentros(null, null, null, null, "az", pageable);

            assertThat(result.getContent().get(0).getProvincia()).isNull();
        }
    }

    private Centro buildCentroCompleto() {
        Centro c = new Centro();
        c.setId(1L);
        c.setUuid(UUID.randomUUID());
        c.setNombreComercial("Academia Test");
        c.setLocalidad("Madrid");
        c.setProvincia(new Provincia(28L, "Madrid", "28", new ComunidadAutonoma(13L, "Madrid", "13")));
        c.setTipo(TipoCentro.PRIVADO);
        c.setVerificado(true);
        c.setFormaciones(new HashSet<>());
        return c;
    }

    @Test
    @DisplayName("lanza 404 si el centro no existe")
    void getPerfil_noExiste_lanza404() {
        java.util.UUID uuid = java.util.UUID.randomUUID();
        when(centroRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPerfilByUuid(uuid))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("devuelve perfil completo con formaciones y FAQs")
    void getPerfil_existe_devuelvePerfilCompleto() {
        when(centroRepository.findByUuid(centro.getUuid())).thenReturn(Optional.of(centro));
        when(valoracionRepository.findMediaByCentroId(1L)).thenReturn(4.2);
        when(valoracionRepository.countByCentroId(1L)).thenReturn(8L);
        when(formacionRepository.findActivasByCentroIdConCentro(1L)).thenReturn(List.of());
        when(faqCentroRepository.findByCentroIdOrderByOrdenAsc(1L)).thenReturn(List.of());
        when(faqCentroMapper.toDTOList(any())).thenReturn(List.of());

        CentroPerfilDTO result = service.getPerfilByUuid(centro.getUuid());

        assertThat(result.getNombreComercial()).isEqualTo("Academia Test");
        assertThat(result.getValoracionMedia()).isEqualTo(4.2);
        assertThat(result.getTotalValoraciones()).isEqualTo(8);
        assertThat(result.getFormaciones()).isEmpty();
        assertThat(result.getFaqs()).isEmpty();
    }

    @Test
    @DisplayName("valoracionMedia es null si no hay valoraciones")
    void getPerfil_sinValoraciones_mediaNull() {
        when(centroRepository.findByUuid(centro.getUuid())).thenReturn(Optional.of(centro));
        when(valoracionRepository.findMediaByCentroId(1L)).thenReturn(null);
        when(valoracionRepository.countByCentroId(1L)).thenReturn(0L);
        when(formacionRepository.findActivasByCentroIdConCentro(1L)).thenReturn(List.of());
        when(faqCentroRepository.findByCentroIdOrderByOrdenAsc(1L)).thenReturn(List.of());
        when(faqCentroMapper.toDTOList(any())).thenReturn(List.of());

        CentroPerfilDTO result = service.getPerfilByUuid(centro.getUuid());

        assertThat(result.getValoracionMedia()).isNull();
        assertThat(result.getTotalValoraciones()).isZero();
    }
}
