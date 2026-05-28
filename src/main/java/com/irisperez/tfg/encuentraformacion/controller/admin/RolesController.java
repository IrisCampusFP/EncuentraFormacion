package com.irisperez.tfg.encuentraformacion.controller.admin;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.RolDTO;
import com.irisperez.tfg.encuentraformacion.service.auth.RolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/roles")
@PreAuthorize("hasRole('ADMIN')") // Solo accesible para usuarios con rol ADMIN (protegido también en SecurityConfig)
public class RolesController {

    private final RolService rolService;

    @Autowired
    public RolesController(RolService rolService) {
        this.rolService = rolService;
    }

    // Obtener todos los roles
    @GetMapping
    public ResponseEntity<?> obtenerRolesUsuario() {
        List<RolDTO> roles = rolService.obtenerRolesDTO();
        return ResponseEntity.ok(roles);
    }

}
