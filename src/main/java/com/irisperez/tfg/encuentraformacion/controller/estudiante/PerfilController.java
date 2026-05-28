package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.usuario.CambiarPasswordDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.PerfilUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.auth.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerDatosUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(usuarioService.obtenerUsuarioDTOPorEmail(auth.getName()));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioDTO> actualizarPerfil(
            @Valid @RequestBody PerfilUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(userDetails.getId(), dto));
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cambiarPassword(
            @Valid @RequestBody CambiarPasswordDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        usuarioService.cambiarPassword(userDetails.getId(), dto.getPasswordActual(), dto.getPasswordNueva());
        return ResponseEntity.noContent().build();
    }

}
