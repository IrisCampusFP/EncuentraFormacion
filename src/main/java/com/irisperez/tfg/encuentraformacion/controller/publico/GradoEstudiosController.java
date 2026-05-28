package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.GradoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.service.catalogo.GradoEstudiosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grado-estudios")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class GradoEstudiosController {

    private final GradoEstudiosService gradoEstudiosService;

    // Lista todos los tipos de estudios disponibles (datos de referencia para select del buscador)
    @GetMapping
    public ResponseEntity<List<GradoEstudiosDTO>> listar() {
        return ResponseEntity.ok(gradoEstudiosService.listar());
    }
}
