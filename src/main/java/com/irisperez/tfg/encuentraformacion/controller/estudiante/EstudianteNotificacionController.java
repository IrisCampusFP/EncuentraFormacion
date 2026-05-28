package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.notificacion.NotificacionDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.notificacion.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class EstudianteNotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Page<NotificacionDTO>> getMisNotificaciones(
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificacionService.getMisNotificaciones(userDetails.getId(), pageable));
    }

    @GetMapping("/no-leidas/count")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Map<String, Long>> countNoLeidas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(Map.of("count", notificacionService.countNoLeidas(userDetails.getId())));
    }

    @PutMapping("/{id}/leida")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> marcarLeida(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificacionService.marcarLeida(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/leidas")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> marcarTodasLeidas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificacionService.marcarTodasLeidas(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/por-conversacion/{convId}/leidas")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> marcarLeidasPorConversacion(
            @PathVariable Long convId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificacionService.marcarLeidasPorConversacion(userDetails.getId(), convId);
        return ResponseEntity.noContent().build();
    }
}
