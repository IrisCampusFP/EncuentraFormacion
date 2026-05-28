package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.formacion.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    @GetMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Page<FormacionResumenDTO>> getMisFavoritos(
            @PageableDefault(size = 12) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(favoritoService.getMisFavoritos(userDetails.getId(), pageable));
    }

    @GetMapping("/{formacionUuid}/estado")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Map<String, Boolean>> getEstado(
            @PathVariable UUID formacionUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean guardada = favoritoService.esGuardada(formacionUuid, userDetails.getId());
        return ResponseEntity.ok(Map.of("guardada", guardada));
    }

    @PostMapping("/{formacionUuid}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> agregar(
            @PathVariable UUID formacionUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        favoritoService.agregar(formacionUuid, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{formacionUuid}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> quitar(
            @PathVariable UUID formacionUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        favoritoService.quitar(formacionUuid, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
