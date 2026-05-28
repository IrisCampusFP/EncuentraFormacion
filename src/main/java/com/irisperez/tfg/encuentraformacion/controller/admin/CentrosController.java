package com.irisperez.tfg.encuentraformacion.controller.admin;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.service.centro.CentroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/centros")
@PreAuthorize("hasRole('ADMIN')") // Solo accesible para usuarios con rol ADMIN (protegido también en SecurityConfig)
public class CentrosController {

    private final CentroService centroService;

    @Autowired
    public CentrosController(CentroService centroService) {
        this.centroService = centroService;
    }

    // Obtener todos los centros
    @GetMapping
    public ResponseEntity<?> obtenerCentros() {
        List<CentroDTO> centros = centroService.obtenerCentros();
        return ResponseEntity.ok(centros);
    }

    // Obtener centros verificados paginados con filtros opcionales
    @GetMapping("/verificados")
    public ResponseEntity<Page<CentroDTO>> obtenerCentrosVerificados(
            @PageableDefault(size = 10, sort = "fechaAlta", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long      id,
            @RequestParam(required = false) String    q,
            @RequestParam(required = false) TipoCentro tipo,
            @RequestParam(required = false) Boolean   tieneGestor) {
        return ResponseEntity.ok(centroService.buscarVerificadosConFiltros(id, q, tipo, tieneGestor, pageable));
    }

    // Obtener centros no verificados paginados con filtros opcionales
    @GetMapping("/sin-verificar")
    public ResponseEntity<Page<CentroDTO>> obtenerCentrosNoVerificados(
            @PageableDefault(size = 10, sort = "fechaAlta", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long      id,
            @RequestParam(required = false) String    q,
            @RequestParam(required = false) TipoCentro tipo) {
        return ResponseEntity.ok(centroService.buscarSinVerificarConFiltros(id, q, tipo, pageable));
    }

    // Obtener un centro por su ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCentroPorId(@PathVariable Long id) {
        CentroDTO centro = centroService.obtenerCentroDTOPorId(id);
        return ResponseEntity.ok(centro);
    }

    // Comprobar la existencia de un centro por su código oficial (devuelve 404 si no existe)
    @GetMapping("/existe")
    @PreAuthorize("permitAll()") // EXCEPCIÓN: endpoint público porque se usa en el formulario de registro de gestor (usuario no autenticado)
    public ResponseEntity<?> comprobarCentroPorCodigo(@RequestParam String codigo) {
        if (!centroService.existeCodigo(codigo)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(centroService.obtenerCentroDTOPorCodigo(codigo));
    }

    // Crear centro
    @PostMapping
    public ResponseEntity<?> crearCentro(@RequestBody RegistroCentroRequestDTO dto) {
        CentroDTO centroCreado = centroService.registrarCentro(dto);
        centroService.verificarCentro(centroCreado.getId()); // Los centros creados desde el admin son directamente verificados
        return ResponseEntity.status(HttpStatus.CREATED).body(centroService.obtenerCentroDTOPorId(centroCreado.getId()));
    }

    // Editar centro
    @PutMapping("/{id}")
    public ResponseEntity<?> editarCentro(@PathVariable Long id, @RequestBody CentroUpdateDTO dto) {
        CentroDTO centroActualizado = centroService.actualizarCentro(id, dto);
        return ResponseEntity.ok(centroActualizado);
    }

    // Eliminar centro
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCentro(@PathVariable Long id) {
        centroService.eliminarCentro(id);
        return ResponseEntity.ok().build();
    }

    // Verificar centro
    @PutMapping("/{id}/verificar")
    public ResponseEntity<?> verificarCentro(@PathVariable Long id) {
        centroService.verificarCentro(id);
        return ResponseEntity.ok().build();
    }

    // Quitar verificación de centro
    @PutMapping("/{id}/quitar-verificacion")
    public ResponseEntity<?> quitarVerificacionCentro(@PathVariable Long id) {
        centroService.quitarVerificacionCentro(id);
        return ResponseEntity.ok().build();
    }

    // Obtener gestores de un centro
    @GetMapping("/{id}/gestores")
    public ResponseEntity<List<UsuarioDTO>> obtenerGestoresDeCentro(@PathVariable Long id) {
        return ResponseEntity.ok(centroService.obtenerGestoresDeCentro(id));
    }
}
