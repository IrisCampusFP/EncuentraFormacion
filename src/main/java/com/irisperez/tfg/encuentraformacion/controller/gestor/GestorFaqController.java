package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.faq.CrearFaqDTO;
import com.irisperez.tfg.encuentraformacion.dto.faq.EditarFaqDTO;
import com.irisperez.tfg.encuentraformacion.dto.faq.FaqGestorDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.faq.GestorFaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/gestor/faq")
@RequiredArgsConstructor
public class GestorFaqController {

    private final GestorFaqService service;

    @GetMapping
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<List<FaqGestorDTO>> getFaqs(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(service.getFaqs(user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<FaqGestorDTO> crear(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CrearFaqDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(user.getId(), dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<FaqGestorDTO> editar(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody EditarFaqDTO dto) {
        return ResponseEntity.ok(service.editar(user.getId(), id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        service.eliminar(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
