package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.asistente.*;
import org.springframework.web.bind.annotation.PatchMapping;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.asistente.AsistenteIAService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asistente")
@RequiredArgsConstructor
public class AsistenteIAController {

    private final AsistenteIAService asistenteService;

    @PostMapping("/sesiones")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<SesionIAResumenDTO> crearSesion(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asistenteService.crearSesion(user.getId()));
    }

    @GetMapping("/sesiones")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<SesionIAResumenDTO>> listarSesiones(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(asistenteService.listarSesiones(user.getId()));
    }

    @PostMapping("/sesiones/{id}/mensajes")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<RespuestaAsistenteDTO> enviarMensaje(
            @PathVariable Long id,
            @Valid @RequestBody EnviarMensajeIADTO dto,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(asistenteService.enviarMensaje(id, dto.getContenido(), user.getId()));
    }

    @GetMapping("/sesiones/{id}/historial")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<HistorialSesionDTO> getHistorial(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(asistenteService.getHistorial(id, user.getId()));
    }

    @PatchMapping("/sesiones/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<SesionIAResumenDTO> renombrarSesion(
            @PathVariable Long id,
            @Valid @RequestBody RenombrarSesionDTO dto,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(asistenteService.renombrarSesion(id, dto.getTitulo(), user.getId()));
    }

    @DeleteMapping("/sesiones")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> eliminarTodasLasSesiones(@AuthenticationPrincipal CustomUserDetails user) {
        asistenteService.eliminarTodasLasSesiones(user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sesiones/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> eliminarSesion(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        asistenteService.eliminarSesion(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
