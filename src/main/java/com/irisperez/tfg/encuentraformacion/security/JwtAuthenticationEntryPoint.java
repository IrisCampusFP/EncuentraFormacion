package com.irisperez.tfg.encuentraformacion.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Clase que captura el error de autenticación (401 Unauthorized) y devuelve una respuesta JSON

 * Si el usuario intenta acceder a un recurso protegido sin un token válido, la API 
 * devuelve un error 401 Unauthorized (en vez de redirigir a una página de login, 
 * que es el comportamiento por defecto de Spring Security)
 */
@Component
@NullMarked
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Metodo que se ejecuta automáticamente cuando hay un error de autenticación
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        // Delegamos el error en Spring Boot enviando el código 401 (Unauthorized)
        // (Spring usará automáticamente 401.html si es un navegador o devolverá un JSON si es una petición de API).
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }
}
