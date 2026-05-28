package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ComunidadAutonomaDTO;
import com.irisperez.tfg.encuentraformacion.service.catalogo.ComunidadAutonomaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/comunidades-autonomas")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class ComunidadAutonomaController {

    private final ComunidadAutonomaService comunidadAutonomaService;

    @GetMapping
    public ResponseEntity<List<ComunidadAutonomaDTO>> listar() {
        return ResponseEntity.ok(comunidadAutonomaService.listar());
    }
}
