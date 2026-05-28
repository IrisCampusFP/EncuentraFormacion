package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.EditarCentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.centro.GestorCentroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gestor/centro")
@RequiredArgsConstructor
public class GestorCentroController {

    private final GestorCentroService service;

    @GetMapping
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<CentroGestorDTO> getCentro(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(service.getCentro(user.getId()));
    }

    @PutMapping
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<CentroGestorDTO> editarCentro(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody EditarCentroGestorDTO dto) {
        return ResponseEntity.ok(service.editarCentro(user.getId(), dto));
    }
}
