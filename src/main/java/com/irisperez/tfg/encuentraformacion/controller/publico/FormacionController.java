package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionDetalleDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionFiltroDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.service.formacion.FormacionPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/formaciones")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class FormacionController {

    private final FormacionPublicService formacionPublicService;

    // Buscador principal — sortBy semántico evita exponer nombres de campo al cliente
    @GetMapping
    public ResponseEntity<Page<FormacionResumenDTO>> buscar(
            FormacionFiltroDTO filtro,
            @RequestParam(defaultValue = "recientes") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, resolverOrden(sortBy));
        return ResponseEntity.ok(formacionPublicService.buscar(filtro, pageable, sortBy));
    }

    private Sort resolverOrden(String sortBy) {
        return switch (sortBy) {
            case "precioAsc"      -> Sort.by(Sort.Order.asc("precio").with(Sort.NullHandling.NULLS_LAST));
            case "precioDesc"     -> Sort.by(Sort.Order.desc("precio").with(Sort.NullHandling.NULLS_LAST));
            case "proximasFechas" -> Sort.by(Sort.Order.asc("fechaInicio").with(Sort.NullHandling.NULLS_LAST));
            case "az"             -> Sort.by(Sort.Order.asc("nombre"));
            case "za"             -> Sort.by(Sort.Order.desc("nombre"));
            default               -> Sort.by(Sort.Order.desc("fechaAlta")); // recientes y valoracion
        };
    }

    // Autocomplete de títulos oficiales para el buscador (público, sin autenticación)
    @GetMapping("/titulos-oficiales")
    public ResponseEntity<List<String>> titulosOficiales(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(formacionPublicService.buscarTitulosOficiales(q));
    }

    // Detalle completo de una formación con valoraciones incluidas
    @GetMapping("/{uuid}")
    public ResponseEntity<FormacionDetalleDTO> detalle(@PathVariable UUID uuid) {
        return ResponseEntity.ok(formacionPublicService.findByUuid(uuid));
    }
}
