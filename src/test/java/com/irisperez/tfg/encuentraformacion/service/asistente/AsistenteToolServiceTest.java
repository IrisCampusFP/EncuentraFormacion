package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionDetalleDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionFiltroDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import com.irisperez.tfg.encuentraformacion.repository.ComunidadAutonomaRepository;
import com.irisperez.tfg.encuentraformacion.repository.TipoEstudiosRepository;
import com.irisperez.tfg.encuentraformacion.repository.ProvinciaRepository;
import com.irisperez.tfg.encuentraformacion.service.formacion.FormacionPublicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsistenteToolService")
class AsistenteToolServiceTest {

    @Mock private FormacionPublicService formacionPublicService;
    @Mock private TipoEstudiosRepository tipoEstudiosRepository;
    @Mock private ProvinciaRepository provinciaRepository;
    @Mock private ComunidadAutonomaRepository comunidadAutonomaRepository;

    @InjectMocks private AsistenteToolService service;

    @Test
    @DisplayName("buscarFormaciones con resultados devuelve texto formateado")
    void buscarFormacionesConResultadosDevuelveTextoFormateado() {
        FormacionResumenDTO dto = new FormacionResumenDTO();
        dto.setNombre("Desarrollo Web");
        dto.setUuid(UUID.randomUUID());

        Page<FormacionResumenDTO> page = new PageImpl<>(List.of(dto));
        when(formacionPublicService.buscar(any(FormacionFiltroDTO.class), any(PageRequest.class), anyString()))
            .thenReturn(page);

        com.fasterxml.jackson.databind.node.ObjectNode args =
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();

        String resultado = service.ejecutar("buscarFormaciones", args);

        assertThat(resultado).contains("Resultados");
        assertThat(resultado).contains("Desarrollo Web");
    }

    @Test
    @DisplayName("buscarFormaciones sin resultados devuelve mensaje vacío")
    void buscarFormacionesSinResultadosDevuelveMensajeVacio() {
        Page<FormacionResumenDTO> emptyPage = Page.empty();
        when(formacionPublicService.buscar(any(FormacionFiltroDTO.class), any(PageRequest.class), anyString()))
            .thenReturn(emptyPage);

        com.fasterxml.jackson.databind.node.ObjectNode args =
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();

        String resultado = service.ejecutar("buscarFormaciones", args);

        assertThat(resultado).contains("No se encontraron formaciones");
    }

    @Test
    @DisplayName("buscarFormaciones con tipo de estudios desconocido ignora filtro")
    void buscarFormacionesConTipoEstudiosDesconocidoIgnoraFiltro() {
        when(tipoEstudiosRepository.findByNombreIgnoreCase("Desconocido")).thenReturn(Optional.empty());
        Page<FormacionResumenDTO> page = new PageImpl<>(List.of());
        when(formacionPublicService.buscar(any(FormacionFiltroDTO.class), any(PageRequest.class), anyString()))
            .thenReturn(page);

        com.fasterxml.jackson.databind.node.ObjectNode args =
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        args.put("tipoEstudiosNombre", "Desconocido");

        String resultado = service.ejecutar("buscarFormaciones", args);

        assertThat(resultado).isNotNull();
    }

    @Test
    @DisplayName("buscarFormaciones con provincia desconocida ignora filtro")
    void buscarFormacionesConProvinciaDesconocidaIgnoraFiltro() {
        when(provinciaRepository.findByNombreIgnoreCase("Narnia")).thenReturn(Optional.empty());
        Page<FormacionResumenDTO> page = new PageImpl<>(List.of());
        when(formacionPublicService.buscar(any(FormacionFiltroDTO.class), any(PageRequest.class), anyString()))
            .thenReturn(page);

        com.fasterxml.jackson.databind.node.ObjectNode args =
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        args.put("provincia", "Narnia");

        String resultado = service.ejecutar("buscarFormaciones", args);

        assertThat(resultado).isNotNull();
    }

    @Test
    @DisplayName("getDetalleFormacion con UUID válido devuelve texto")
    void getDetalleFormacionConUuidValidoDevuelveTexto() {
        UUID uuid = UUID.randomUUID();
        FormacionDetalleDTO detalle =
            new FormacionDetalleDTO();
        detalle.setNombre("Grado en Informática");
        detalle.setUuid(uuid);

        when(formacionPublicService.findByUuid(uuid)).thenReturn(detalle);

        com.fasterxml.jackson.databind.node.ObjectNode args =
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        args.put("formacionUuid", uuid.toString());

        String resultado = service.ejecutar("getDetalleFormacion", args);

        assertThat(resultado).contains("Grado en Informática");
    }

    @Test
    @DisplayName("getDetalleFormacion con UUID inválido devuelve texto de error")
    void getDetalleFormacionConUuidInvalidoDevuelveTextoDeError() {
        com.fasterxml.jackson.databind.node.ObjectNode args =
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        args.put("formacionUuid", "no-es-un-uuid");

        String resultado = service.ejecutar("getDetalleFormacion", args);

        assertThat(resultado).containsIgnoringCase("error").containsIgnoringCase("UUID");
    }

    @Test
    @DisplayName("buscarFormaciones con comunidad autónoma desconocida ignora filtro")
    void buscarFormacionesConComunidadAutonomaDesconocidaIgnoraFiltro() {
        when(comunidadAutonomaRepository.findByNombreIgnoreCase("Mordor")).thenReturn(Optional.empty());
        Page<FormacionResumenDTO> page = new PageImpl<>(List.of());
        when(formacionPublicService.buscar(any(FormacionFiltroDTO.class), any(PageRequest.class), anyString()))
            .thenReturn(page);

        com.fasterxml.jackson.databind.node.ObjectNode args =
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        args.put("comunidadAutonoma", "Mordor");

        String resultado = service.ejecutar("buscarFormaciones", args);

        assertThat(resultado).isNotNull();
    }

    @Test
    @DisplayName("buscarFormaciones normaliza 'Comunidad de Madrid' al nombre INE de la BD")
    void buscarFormacionesNormalizaNombreColoquialCCAA() {
        ComunidadAutonoma madrid = new ComunidadAutonoma();
        madrid.setId(13L);
        madrid.setNombre("Madrid, Comunidad de");

        // El LLM envía "Comunidad de Madrid"; el servicio debe resolver a "Madrid, Comunidad de"
        when(comunidadAutonomaRepository.findByNombreIgnoreCase("Madrid, Comunidad de"))
            .thenReturn(Optional.of(madrid));
        Page<FormacionResumenDTO> page = new PageImpl<>(List.of());
        when(formacionPublicService.buscar(any(FormacionFiltroDTO.class), any(PageRequest.class), anyString()))
            .thenReturn(page);

        com.fasterxml.jackson.databind.node.ObjectNode args =
            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        args.put("comunidadAutonoma", "Comunidad de Madrid");

        service.ejecutar("buscarFormaciones", args);

        verify(comunidadAutonomaRepository).findByNombreIgnoreCase("Madrid, Comunidad de");
    }
}
