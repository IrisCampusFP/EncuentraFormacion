package com.irisperez.tfg.encuentraformacion.service.formacion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionDetalleDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionFiltroDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.mapper.formacion.FormacionMapper;
import com.irisperez.tfg.encuentraformacion.mapper.valoracion.ValoracionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Valoracion;
import com.irisperez.tfg.encuentraformacion.repository.FormacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.ValoracionRepository;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FormacionPublicService")
class FormacionPublicServiceTest {

    @Mock private FormacionRepository formacionRepository;
    @Mock private FormacionMapper formacionMapper;
    @Mock private ValoracionRepository valoracionRepository;
    @Mock private ValoracionMapper valoracionMapper;
    @Mock private ValoracionEstudianteService valoracionService;
    @Mock private EntityManager entityManager;

    @InjectMocks private FormacionPublicService service;

    @Nested
    @DisplayName("buscar()")
    class BuscarTests {

        @Test
        @DisplayName("busca con sortBy por defecto usando Specification")
        void buscar_sinFiltros_usaSpecification() {
            Pageable pageable = PageRequest.of(0, 10);
            Formacion f = new Formacion(); f.setId(1L);
            FormacionResumenDTO dto = new FormacionResumenDTO(); dto.setId(1L);
            Page<Formacion> pageFormaciones = new PageImpl<>(List.of(f));

            when(formacionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(pageFormaciones);
            when(formacionMapper.toResumenDTO(f)).thenReturn(dto);

            Page<FormacionResumenDTO> result = service.buscar(null, pageable, "az");

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("busca con sortBy=valoracion usando query especial")
        void buscar_sortValoracion() {
            Pageable pageable = PageRequest.of(0, 10);
            Formacion f = new Formacion(); f.setId(2L);
            FormacionResumenDTO dto = new FormacionResumenDTO(); dto.setId(2L);
            Page<Formacion> pageFormaciones = new PageImpl<>(List.of(f));

            when(formacionRepository.findOrdenadosPorValoracion(
                anyBoolean(), anyString(), anyBoolean(), anyString(), anyBoolean(), anyString(),
                any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), eq(pageable)
            )).thenReturn(pageFormaciones);
            when(formacionMapper.toResumenDTO(f)).thenReturn(dto);

            Page<FormacionResumenDTO> result = service.buscar(null, pageable, "valoracion");

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("delega el enriquecimiento de valoraciones a ValoracionEstudianteService")
        void buscar_delegaCargarValoraciones() {
            Pageable pageable = PageRequest.of(0, 10);
            Formacion f = new Formacion(); f.setId(3L);
            FormacionResumenDTO dto = new FormacionResumenDTO(); dto.setId(3L);
            Page<Formacion> pageFormaciones = new PageImpl<>(List.of(f));

            when(formacionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(pageFormaciones);
            when(formacionMapper.toResumenDTO(f)).thenReturn(dto);

            service.buscar(null, pageable, "az");

            verify(valoracionService).cargarValoraciones(List.of(dto));
        }

        @Test
        @DisplayName("buscar con filtros soloGratuitas=true fuerza precio null")
        void buscar_soloGratuitas_conValoracion() {
            Pageable pageable = PageRequest.of(0, 10);
            FormacionFiltroDTO filtro = new FormacionFiltroDTO();
            filtro.setSoloGratuitas(true);
            filtro.setNombre("Java");

            Page<Formacion> pageVacia = new PageImpl<>(List.of());
            when(formacionRepository.findOrdenadosPorValoracion(
                anyBoolean(), anyString(), anyBoolean(), anyString(), anyBoolean(), anyString(),
                any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), eq(pageable)
            )).thenReturn(pageVacia);

            Page<FormacionResumenDTO> result = service.buscar(filtro, pageable, "valoracion");

            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findByUuid()")
    class FindByUuidTests {

        @Test
        @DisplayName("devuelve detalle completo con valoraciones")
        void findByUuid_ok() {
            UUID uuid = UUID.randomUUID();
            Formacion f = new Formacion(); f.setId(10L); f.setUuid(uuid);
            FormacionDetalleDTO dto = new FormacionDetalleDTO(); dto.setUuid(uuid);

            Valoracion v = new Valoracion(); v.setEstrellas(5);
            when(formacionRepository.findByUuid(uuid)).thenReturn(Optional.of(f));
            when(formacionMapper.toDetalleDTO(f)).thenReturn(dto);
            when(valoracionRepository.findByFormacionIdConEstudiante(10L)).thenReturn(List.of(v));
            when(valoracionMapper.toDTOList(List.of(v))).thenReturn(List.of());

            FormacionDetalleDTO result = service.findByUuid(uuid);

            assertThat(result.getTotalValoraciones()).isEqualTo(1L);
            assertThat(result.getValoracionMedia()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("devuelve detalle sin valoraciones (media null)")
        void findByUuid_sinValoraciones() {
            UUID uuid = UUID.randomUUID();
            Formacion f = new Formacion(); f.setId(11L); f.setUuid(uuid);
            FormacionDetalleDTO dto = new FormacionDetalleDTO();

            when(formacionRepository.findByUuid(uuid)).thenReturn(Optional.of(f));
            when(formacionMapper.toDetalleDTO(f)).thenReturn(dto);
            when(valoracionRepository.findByFormacionIdConEstudiante(11L)).thenReturn(List.of());
            when(valoracionMapper.toDTOList(List.of())).thenReturn(List.of());

            FormacionDetalleDTO result = service.findByUuid(uuid);

            assertThat(result.getTotalValoraciones()).isZero();
            assertThat(result.getValoracionMedia()).isNull();
        }

        @Test
        @DisplayName("404 si la formación no existe")
        void findByUuid_noExiste_lanza404() {
            UUID uuid = UUID.randomUUID();
            when(formacionRepository.findByUuid(uuid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByUuid(uuid))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("buscarTitulosOficiales()")
    class BuscarTitulosTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("devuelve lista de títulos oficiales")
        void buscarTitulosOficiales_ok() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<String> cq = mock(CriteriaQuery.class);
            Root<Formacion> root = mock(Root.class);
            Join<Formacion, ?> join = mock(Join.class);
            Path<Boolean> activaPath = mock(Path.class);
            Path<String> tituloPath = mock(Path.class);
            Expression<String> lowerExpr = mock(Expression.class);
            Expression<String> unaccentExpr = mock(Expression.class);
            Predicate predTrue = mock(Predicate.class);
            Predicate predLike = mock(Predicate.class);
            Order order = mock(Order.class);
            TypedQuery<String> typedQuery = mock(TypedQuery.class);

            when(entityManager.getCriteriaBuilder()).thenReturn(cb);
            when(cb.createQuery(String.class)).thenReturn(cq);
            when(cq.from(Formacion.class)).thenReturn(root);
            when(root.join(eq("centro"), any(JoinType.class))).thenReturn((Join) join);
            when(root.<Boolean>get("activa")).thenReturn(activaPath);
            when(root.<String>get("tituloOficial")).thenReturn(tituloPath);
            when(cb.isTrue(activaPath)).thenReturn(predTrue);
            when(cb.isNotNull(tituloPath)).thenReturn(predTrue);
            when(cb.notEqual(any(), any())).thenReturn(predTrue);
            when(cb.lower(any())).thenReturn(lowerExpr);
            when(cb.function(eq("unaccent"), eq(String.class), any())).thenReturn(unaccentExpr);
            when(cb.like(any(), anyString())).thenReturn(predLike);
            when(cq.select(any())).thenReturn(cq);
            when(cq.distinct(anyBoolean())).thenReturn(cq);
            when(cq.where(any(Predicate[].class))).thenReturn(cq);
            when(cb.asc(any())).thenReturn(order);
            when(cq.orderBy(any(Order.class))).thenReturn(cq);
            when(entityManager.createQuery(cq)).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(20)).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of("Bachillerato en Artes"));

            List<String> result = service.buscarTitulosOficiales("artes");

            assertThat(result).containsExactly("Bachillerato en Artes");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("acepta query nula o vacía")
        void buscarTitulosOficiales_queryNula() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<String> cq = mock(CriteriaQuery.class);
            Root<Formacion> root = mock(Root.class);
            Join<Formacion, ?> join = mock(Join.class);
            Path<Boolean> activaPath = mock(Path.class);
            Path<String> tituloPath = mock(Path.class);
            Expression<String> lowerExpr = mock(Expression.class);
            Expression<String> unaccentExpr = mock(Expression.class);
            Predicate pred = mock(Predicate.class);
            Order order = mock(Order.class);
            TypedQuery<String> typedQuery = mock(TypedQuery.class);

            when(entityManager.getCriteriaBuilder()).thenReturn(cb);
            when(cb.createQuery(String.class)).thenReturn(cq);
            when(cq.from(Formacion.class)).thenReturn(root);
            when(root.join(eq("centro"), any(JoinType.class))).thenReturn((Join) join);
            when(root.<Boolean>get("activa")).thenReturn(activaPath);
            when(root.<String>get("tituloOficial")).thenReturn(tituloPath);
            when(cb.isTrue(activaPath)).thenReturn(pred);
            when(cb.isNotNull(tituloPath)).thenReturn(pred);
            when(cb.notEqual(any(), any())).thenReturn(pred);
            when(cb.lower(any())).thenReturn(lowerExpr);
            when(cb.function(eq("unaccent"), eq(String.class), any())).thenReturn(unaccentExpr);
            when(cb.like(any(), anyString())).thenReturn(pred);
            when(cq.select(any())).thenReturn(cq);
            when(cq.distinct(anyBoolean())).thenReturn(cq);
            when(cq.where(any(Predicate[].class))).thenReturn(cq);
            when(cb.asc(any())).thenReturn(order);
            when(cq.orderBy(any(Order.class))).thenReturn(cq);
            when(entityManager.createQuery(cq)).thenReturn(typedQuery);
            when(typedQuery.setMaxResults(20)).thenReturn(typedQuery);
            when(typedQuery.getResultList()).thenReturn(List.of());

            assertThat(service.buscarTitulosOficiales(null)).isEmpty();
        }
    }
}
