package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.CrearSolicitudDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.SolicitudResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.solicitud.formacion.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/solicitudes-formacion")
@RequiredArgsConstructor
public class SolicitudFormacionController {

    private final SolicitudService solicitudService;

    @GetMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Page<SolicitudResumenDTO>> getMisSolicitudes(
            @RequestParam(required = false) EstadoSolicitud estado,
            @PageableDefault(size = 10, sort = "fechaSolicitud") Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudService.getMisSolicitudes(userDetails.getId(), estado, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<SolicitudResumenDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudService.getById(id, userDetails.getId()));
    }

    @GetMapping("/check/{formacionUuid}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<SolicitudResumenDTO> check(
            @PathVariable UUID formacionUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return solicitudService.checkByFormacionUuid(formacionUuid, userDetails.getId())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<SolicitudResumenDTO> crear(
            @Valid @RequestBody CrearSolicitudDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(solicitudService.crear(dto, userDetails.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        solicitudService.cancelar(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
