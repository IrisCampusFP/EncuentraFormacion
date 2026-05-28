package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroBuscadorDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroPerfilDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.service.centro.CentroPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class CentroPublicController {

    private final CentroPublicService centroPublicService;

    // Buscador público de centros verificados
    @GetMapping("/centros/buscar")
    public ResponseEntity<Page<CentroBuscadorDTO>> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String localidad,
            @RequestParam(required = false) Long provinciaId,
            @RequestParam(required = false) TipoCentro tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "az") String sortBy) {
        var pageable = switch (sortBy) {
            case "za"       -> PageRequest.of(page, size, Sort.by(Sort.Order.desc("nombreComercial")));
            case "valorados" -> PageRequest.of(page, size);
            default          -> PageRequest.of(page, size, Sort.by(Sort.Order.asc("nombreComercial")));
        };
        return ResponseEntity.ok(centroPublicService.buscarCentros(nombre, localidad, provinciaId, tipo, sortBy, pageable));
    }

    // Perfil público de un centro con sus formaciones activas y preguntas frecuentes (FAQ)
    @GetMapping("/centros/{uuid}/perfil")
    public ResponseEntity<CentroPerfilDTO> getPerfil(@PathVariable UUID uuid) {
        return ResponseEntity.ok(centroPublicService.getPerfilByUuid(uuid));
    }

    // Formaciones activas paginadas de un centro (para carga progresiva en el perfil)
    @GetMapping("/centros/{uuid}/formaciones")
    public ResponseEntity<Page<FormacionResumenDTO>> getFormaciones(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        size = Math.min(size, 20);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("fechaAlta")));
        return ResponseEntity.ok(centroPublicService.getFormacionesPaginadas(uuid, pageable));
    }
}
