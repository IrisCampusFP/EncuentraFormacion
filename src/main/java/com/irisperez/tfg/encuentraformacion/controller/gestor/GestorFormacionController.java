package com.irisperez.tfg.encuentraformacion.controller.gestor;

import com.irisperez.tfg.encuentraformacion.dto.formacion.CrearFormacionDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.EditarFormacionDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.formacion.GestorFormacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gestor/formaciones")
@RequiredArgsConstructor
public class GestorFormacionController {

    private final GestorFormacionService service;

    @GetMapping
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<Page<FormacionGestorDTO>> getFormaciones(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 3, sort = {"activa", "fechaAlta"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.getFormaciones(user.getId(), pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<FormacionGestorDTO> crear(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CrearFormacionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(user.getId(), dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<FormacionGestorDTO> editar(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody EditarFormacionDTO dto) {
        return ResponseEntity.ok(service.editar(user.getId(), id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR_CENTRO')")
    public ResponseEntity<Void> desactivar(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        service.desactivar(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
