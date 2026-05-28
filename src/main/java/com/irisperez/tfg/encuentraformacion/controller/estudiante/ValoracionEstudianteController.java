package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.valoracion.CrearValoracionDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.EditarValoracionDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.ValoracionDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/valoraciones")
@RequiredArgsConstructor
public class ValoracionEstudianteController {

    private final ValoracionEstudianteService valoracionEstudianteService;

    @PostMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<ValoracionDTO> crear(
            @Valid @RequestBody CrearValoracionDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(valoracionEstudianteService.crear(dto, userDetails.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<ValoracionDTO> editar(
            @PathVariable Long id,
            @Valid @RequestBody EditarValoracionDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(valoracionEstudianteService.editar(id, dto, userDetails.getId()));
    }

    @GetMapping("/usuario/{formacionUuid}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<ValoracionDTO> miValoracion(
            @PathVariable UUID formacionUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ValoracionDTO dto = valoracionEstudianteService.miValoracion(formacionUuid, userDetails.getId());
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }
}
