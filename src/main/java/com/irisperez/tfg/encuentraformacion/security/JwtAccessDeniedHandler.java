package com.irisperez.tfg.encuentraformacion.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Clase que captura el error de acceso denegado (403 Forbidden).

 * Se ejecuta cuando un usuario autenticado intenta acceder a un recurso 
 * para el cual no tiene los roles necesarios.
 */
@Component
@NullMarked
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityAuditService auditService;

    public JwtAccessDeniedHandler(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        // Registro de auditoría de seguridad
        auditService.accesoDenegado(request.getRequestURI(), request.getRemoteAddr());

        // Delegamos el error en Spring Boot enviando el código 403 (Forbidden)
        // (Spring mostrará 403.html en navegador o devolverá un JSON a modo de API)
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tienes los permisos necesarios.");
    }
}
