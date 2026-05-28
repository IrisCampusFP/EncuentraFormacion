package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.CambiarEstadoSolicitudDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestorDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.solicitud.formacion.GestorSolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gestor/solicitudes")
@RequiredArgsConstructor
public class GestorSolicitudController {

    private final GestorSolicitudService service;

    @GetMapping
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<Page<SolicitudGestorDTO>> getSolicitudes(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Long formacionId,
            @RequestParam(required = false) java.util.List<EstadoSolicitud> estados,
            @RequestParam(required = false) String nombreEstudiante,
            @PageableDefault(size = 20, sort = "fechaSolicitud", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.getSolicitudes(user.getId(), formacionId, estados, nombreEstudiante, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<SolicitudGestorDTO> getById(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getById(user.getId(), id));
    }

    @GetMapping("/count-pendientes")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<Long> countPendientes(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(service.countPendientes(user.getId()));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<Long> countByEstado(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam EstadoSolicitud estado,
            @RequestParam(required = false) Long formacionId,
            @RequestParam(required = false) String nombreEstudiante) {
        return ResponseEntity.ok(service.countByEstadoWithFilters(user.getId(), estado, formacionId, nombreEstudiante));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<SolicitudGestorDTO> cambiarEstado(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoSolicitudDTO dto) {
        return ResponseEntity.ok(service.cambiarEstado(user.getId(), id, dto));
    }
}
