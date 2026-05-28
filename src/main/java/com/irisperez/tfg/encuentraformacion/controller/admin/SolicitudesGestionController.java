package com.irisperez.tfg.encuentraformacion.controller.admin;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.NuevaSolicitudGestionDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestionDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.service.solicitud.gestion.SolicitudGestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/solicitudes-gestion")
public class SolicitudesGestionController {

    private final SolicitudGestionService solicitudGestionService;

    @Autowired
    public SolicitudesGestionController(SolicitudGestionService solicitudGestionService) {
        this.solicitudGestionService = solicitudGestionService;
    }

    // Obtener historial de solicitudes (paginado) con filtros opcionales
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SolicitudGestionDTO>> obtenerSolicitudesProcesadas(
            @PageableDefault(size = 10, sort = "fechaSolicitud", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String           q,
            @RequestParam(required = false) EstadoSolicitud  estado) {
        return ResponseEntity.ok(solicitudGestionService.buscarHistorialConFiltros(q, estado, pageable));
    }

    // Obtener solicitudes pendientes (paginado) con filtros opcionales
    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SolicitudGestionDTO>> obtenerSolicitudesPendientes(
            @PageableDefault(size = 10, sort = "fechaSolicitud", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String  q,
            @RequestParam(required = false) Boolean verificadoCentro) {
        return ResponseEntity.ok(solicitudGestionService.buscarPendientesConFiltro(q, verificadoCentro, pageable));
    }

    // Listar todas las solicitudes del usuario autenticado
    @GetMapping("/mis-solicitudes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SolicitudGestionDTO>> listarMisSolicitudes() {
        return ResponseEntity.ok(solicitudGestionService.listarSolicitudesDelUsuario());
    }

    // Enviar solicitud de gestión para un centro ya registrado
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SolicitudGestionDTO> enviarSolicitud(
            @Valid @RequestPart("datos") NuevaSolicitudGestionDTO datos,
            @RequestPart("pruebaTitularidad") MultipartFile pruebaTitularidad) throws IOException {
        SolicitudGestionDTO creada = solicitudGestionService.enviarSolicitudUsuarioAutenticado(
                datos.getCentroId(), pruebaTitularidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Registrar centro nuevo y enviar solicitud de gestión en una sola transacción
    @PostMapping(value = "/con-centro-nuevo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SolicitudGestionDTO> enviarSolicitudConCentroNuevo(
            @Valid @RequestPart("datosCentro") RegistroCentroRequestDTO datosCentro,
            @RequestPart("pruebaTitularidad") MultipartFile pruebaTitularidad) throws IOException {
        SolicitudGestionDTO creada = solicitudGestionService.enviarSolicitudConCentroNuevo(
                datosCentro, pruebaTitularidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Obtener una solicitud concreta del usuario autenticado (con validación de propiedad)
    @GetMapping("/mis-solicitudes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SolicitudGestionDTO> obtenerMiSolicitudPorId(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudGestionService.obtenerSolicitudDelUsuarioPorId(id));
    }

    // Obtener la solicitud pendiente más reciente del usuario autenticado (compatibilidad con estado-solicitud.html sin ?id)
    @GetMapping("/mi-solicitud")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerMiSolicitud() {
        SolicitudGestionDTO miSolicitud = solicitudGestionService.obtenerSolicitudDelUsuario();
        return ResponseEntity.ok(miSolicitud);
    }

    // Obtener los datos del centro de la solicitud pendiente más reciente del usuario autenticado
    @GetMapping("/mi-centro")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerMiCentro() {
        SolicitudGestionDTO miSolicitud = solicitudGestionService.obtenerSolicitudDelUsuario();
        return ResponseEntity.ok(solicitudGestionService.obtenerCentroDeSolicitud(miSolicitud.getId()));
    }

    // Obtener los datos del centro de una solicitud propia (con validación de propiedad)
    @GetMapping("/mis-solicitudes/{id}/centro")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerCentroDePropiaSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudGestionService.obtenerCentroDePropiaSolicitud(id));
    }

    // Obtener prueba de titularidad de una solicitud (imagen o PDF)
    @GetMapping("/{id}/imagen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ByteArrayResource> obtenerPruebaTitularidad(@PathVariable Long id) {
        byte[] bytes = solicitudGestionService.obtenerPruebaTitularidad(id);

        String contentType;
        if (bytes.length > 3
                && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4E    && bytes[3] == 0x47) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (bytes.length > 3
                && bytes[0] == 0x25 && bytes[1] == 0x50
                && bytes[2] == 0x44 && bytes[3] == 0x46) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
        } else {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    // Aprobar solicitud
    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')") // Solo para administradores
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id) {
        SolicitudGestionDTO aprobada = solicitudGestionService.aprobarSolicitud(id);
        return ResponseEntity.ok(aprobada);
    }

    // Rechazar solicitud
    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')") // Solo para administradores
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Long id) {
        SolicitudGestionDTO rechazada = solicitudGestionService.rechazarSolicitud(id);
        return ResponseEntity.ok(rechazada);
    }

    // Cancelar solicitud (el usuario solo puede cancelar su propia solicitud)
    // Cualquier usuario autenticado puede acceder al endpoint (el servicio verifica que el usuario sea el dueño de la solicitud)
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelarSolicitud(@PathVariable Long id) {
        solicitudGestionService.cancelarSolicitud(id);
        return ResponseEntity.ok(Map.of("mensaje", "Solicitud cancelada correctamente."));
    }
}
