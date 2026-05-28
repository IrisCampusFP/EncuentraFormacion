package com.irisperez.tfg.encuentraformacion.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

// La anotación @Slf4j de Lombok inyecta automáticamente un Logger (log) estático en la clase.
// (Es la forma más profesional y limpia de implementar logging en una aplicación Spring Boot moderna)
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Maneja errores de validación de campos (@Valid en los DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleHandlersValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errores = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.warn("Error de validación en la petición: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }

    // Devuelve un error HTTP 400 (Bad Request) para errores de validación de datos
    // (manuales)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Petición incorrecta (400 Bad Request): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("errorMsg", e.getMessage()));
    }

    // Devuelve un error HTTP 409 (Conflict) para errores de lógica de negocio o
    // estado (ej: usuario inactivo)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException e) {
        log.warn("Excepción de conflicto lógico (409 Conflict): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorMsg", e.getMessage()));
    }

    @ExceptionHandler(AsistenteRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleAsistenteRateLimit(AsistenteRateLimitException e) {
        log.warn("Rate limit asistente — Retry-After: {}s", e.getRetryAfter());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", String.valueOf(e.getRetryAfter()))
            .body(Map.of("errorMsg", e.getReason(), "retryAfter", e.getRetryAfter()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String detail = e.getMostSpecificCause().getMessage();
        String msg;
        if (detail != null && detail.contains("chk_centros_codigo_formato")) {
            msg = "El código de centro debe tener exactamente 8 dígitos numéricos.";
        } else if (detail != null && detail.contains("centros_codigo_key")) {
            msg = "Ya existe un centro registrado con ese código.";
        } else if (detail != null && detail.contains("centros_email_key")) {
            msg = "Ya existe un centro registrado con ese correo electrónico.";
        } else {
            msg = "Los datos enviados violan una restricción de integridad.";
        }
        log.warn("Violación de integridad de datos (400): {}", detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("errorMsg", msg));
    }

    // Propaga el código HTTP de ResponseStatusException (404, 409, 422, etc.)
    // sin envolverla en un 500 genérico.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException e) {
        log.warn("ResponseStatusException ({}): {}", e.getStatusCode(), e.getReason());
        String msg = e.getReason() != null ? e.getReason() : e.getMessage();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("errorMsg", msg));
    }

    // Reenvío interno al HTML de la página de error 404 cuando el usuario escribe
    // una ruta que no existe.
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFoundException(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        log.debug("Ruta no encontrada (404 Not Found) - URI solicitada: {}", request.getRequestURI());
        request.getRequestDispatcher("/error/404.html").forward(request, response);
        /*
         * getRequestDispatcher(): obtiene el recurso interno al que se envia
         * forward(): reenvía la petición y su respuesta al recurso indicado
         */
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("Archivo demasiado grande (413): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("errorMsg", "El archivo supera el tamaño máximo permitido (10 MB)."));
    }

    // Devuelve un error HTTP 500 (Internal Server Error) genérico cuando algo falla
    // en el servidor para evitar exponer la traza completa de la excepción al
    // usuario
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleInternalServerError(Exception e) {
        log.error("Fallo interno en el servidor (500 Internal Server Error): ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("errorMsg", "Ha ocurrido un error interno inesperado en el servidor."));
    }

    // Maneja el error de credenciales del login (correo electrónico o contraseña incorrectos)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {
        log.warn("Intento de login fallido (401 Unauthorized)");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("errorMsg", "Correo electrónico o contraseña incorrectos."));
    }

    // Maneja el error de login con usuario deshabilitado
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabledException(DisabledException e) {
        log.warn("Intento de login con cuenta desactivada (403 Forbidden)");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("errorMsg", "Tu cuenta está desactivada. Por favor, contacta con un administrador."));
    }

    // Maneja cualquier otro error de autenticación inesperado (catch-all de seguridad)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Error de autenticación inesperado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("errorMsg", "Error de autenticación. No se ha podido completar el inicio de sesión."));
    }
}
