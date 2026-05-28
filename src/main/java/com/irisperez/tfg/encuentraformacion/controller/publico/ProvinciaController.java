package com.irisperez.tfg.encuentraformacion.controller.publico;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ProvinciaDTO;
import com.irisperez.tfg.encuentraformacion.service.catalogo.ProvinciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/provincias")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class ProvinciaController {

    private final ProvinciaService provinciaService;

    // Lista todas las provincias ordenadas alfabéticamente (datos de referencia para formularios)
    @GetMapping
    public ResponseEntity<List<ProvinciaDTO>> listar() {
        return ResponseEntity.ok(provinciaService.listar());
    }
}
