package com.irisperez.tfg.encuentraformacion.controller.auth;

import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroGestorRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroUsuarioEstudianteRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.service.centro.CentroService;
import com.irisperez.tfg.encuentraformacion.service.solicitud.gestion.SolicitudGestionService;
import com.irisperez.tfg.encuentraformacion.service.auth.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@PreAuthorize("permitAll()")
public class RegistroController {

    private final UsuarioService usuarioService;
    private final SolicitudGestionService solicitudGestionService;
    private final CentroService centroService;

    @Autowired
    public RegistroController(UsuarioService usuarioService, SolicitudGestionService solicitudGestionService,
            CentroService centroService) {
        this.usuarioService = usuarioService;
        this.solicitudGestionService = solicitudGestionService;
        this.centroService = centroService;
    }

    @PostMapping("/registro")
    // @Valid activa la validación de los campos del DTO (anotaciones @NotBlank,
    // @Email, @Size, @Pattern)
    public ResponseEntity<?> registrarUsuario(
            @Valid @RequestBody RegistroUsuarioEstudianteRequestDTO datosUsuarioRegistradoDTO) {
        UsuarioDTO usuarioCreado = usuarioService.registrarEstudiante(datosUsuarioRegistradoDTO);
        return ResponseEntity.ok(usuarioCreado);
    }

    @PostMapping(value = "/registro/gestor", consumes = { "multipart/form-data" })
    public ResponseEntity<?> registrarGestor(
            @RequestPart("datosUsuarioCentro") RegistroGestorRequestDTO datosDTO,
            @RequestPart("pruebaTitularidad") MultipartFile titularidadCentro) throws Exception {
        if (datosDTO.getIdCentroExistente() != null) {
            solicitudGestionService.registrarGestorCentroExistente(datosDTO, titularidadCentro);
        } else {
            solicitudGestionService.registrarGestorCentroNuevo(datosDTO, titularidadCentro);
        }
        return ResponseEntity.ok(Map.of("mensaje", "Usuario registrado y solicitud enviada con éxito."));
    }

    @PostMapping("/registro/centro")
    public ResponseEntity<?> registrarCentro(@RequestBody RegistroCentroRequestDTO datosCentroDTO) {
        centroService.registrarCentro(datosCentroDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Centro registrado correctamente pendiente de verificación."));
    }

    @GetMapping("/check-email-unique")
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        boolean existeEmail = usuarioService.existeEmail(email);
        return Map.of("existe", existeEmail);
    }

    @GetMapping("/check-username-unique")
    public Map<String, Boolean> checkUsername(@RequestParam String username) {
        boolean existeUsername = usuarioService.existeUsername(username);
        return Map.of("existe", existeUsername);
    }

}
