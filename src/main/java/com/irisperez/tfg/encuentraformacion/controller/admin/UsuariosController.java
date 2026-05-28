package com.irisperez.tfg.encuentraformacion.controller.admin;

import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioUpdateDTO;
import com.irisperez.tfg.encuentraformacion.service.auth.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')") // Solo accesible para usuarios con rol ADMIN (protegido también en SecurityConfig)
public class UsuariosController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuariosController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Crear un nuevo usuario
    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@RequestBody RegistroRequestDTO usuario) {
        UsuarioDTO nuevoUsuario = usuarioService.registrarUsuario(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }

    // Obtener usuarios paginados con filtros opcionales
    @GetMapping
    public ResponseEntity<Page<UsuarioDTO>> obtenerUsuarios(
            @PageableDefault(size = 10, sort = {"fechaAlta", "id"}) Pageable pageable,
            @RequestParam(required = false) Long    id,
            @RequestParam(required = false) String  q,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long    rolId) {
        Page<UsuarioDTO> usuarios = usuarioService.buscarConFiltros(id, q, activo, rolId, pageable);
        return ResponseEntity.ok(usuarios);
    }

    // Actualizar usuario
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioUpdateDTO usuarioActualizado) {
        UsuarioDTO usuario = usuarioService.actualizarUsuario(id, usuarioActualizado);
        return ResponseEntity.ok(usuario);
    }

    // Obtener usuario por ID
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorId(@PathVariable Long id) {
        UsuarioDTO usuario = usuarioService.obtenerUsuarioDTOPorId(id);
        return ResponseEntity.ok(usuario);
    }

    // Cambiar estado del usuario (activo/inactivo)
    @PutMapping("/{id}/estado")
    public ResponseEntity<Map<String, Boolean>> cambiarEstadoUsuario(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        boolean nuevoEstado = usuarioService.actualizarEstado(id, activo);
        return ResponseEntity.ok(Map.of("activo", nuevoEstado));
    }

    // Eliminar un usuario
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.ok().build();
    }

    // Cambiar contraseña
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> cambiarPasswordUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String passwordNueva = body.get("passwordNueva");
        usuarioService.cambiarPasswordAdmin(id, passwordNueva);
        return ResponseEntity.ok().build();
    }

    // Asignar roles
    @PutMapping("/{id}/roles")
    public ResponseEntity<Void> actualizarRolesUsuario(@PathVariable Long id, @RequestBody List<Long> idsRoles) {
        usuarioService.actualizarRolesUsuario(id, idsRoles);
        return ResponseEntity.ok().build();
    }
}