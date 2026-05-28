package com.irisperez.tfg.encuentraformacion.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad que se ejecuta una vez por cada petición HTTP.
 * Se encarga de extraer el token JWT del encabezado 'Authorization',
 * validarlo y autenticar al usuario en el contexto de Spring Security.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityAuditService auditService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = null;

        // 1. Intenta obtener el token del encabezado 'Authorization'
        final String encabezadoAuth = request.getHeader("Authorization");
        if (encabezadoAuth != null && encabezadoAuth.startsWith("Bearer ")) {
            token = encabezadoAuth.substring(7); // Quita "Bearer " para quedarse solo con el token
        }
        
        // 2. Si no hay encabezado, intenta obtener el token de la Cookie 'jwt_token'
        if (token == null && request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // Si no hay token en ninguna parte, pasa al siguiente filtro de la cadena
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }


        try {
            // Comprobación de lista negra para prevenir el uso de tokens revocados (Authentication Failures)
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.warn("Intento de uso de token en lista negra en la petición [{}]", LogSanitizer.sanitize(request.getRequestURI()));
                auditService.tokenInvalidado(request.getRemoteAddr());
                filterChain.doFilter(request, response);
                return;
            }

            // Extrae el email del usuario del token
            String userEmail = jwtService.extraerEmail(token);
            
            // Si el email no es nulo y no hay autenticacion en el contexto de seguridad, se autentica al usuario
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado para la petición [{}]", LogSanitizer.sanitize(request.getRequestURI()));
        } catch (JwtException e) {
            log.warn("Token JWT inválido para la petición [{}]", LogSanitizer.sanitize(request.getRequestURI()));
        } catch (Exception e) {
            log.warn("Error inesperado al procesar el token JWT para la petición [{}]", LogSanitizer.sanitize(request.getRequestURI()), e);
        }

        // Pasa al siguiente filtro de la cadena
        filterChain.doFilter(request, response);
    }
}
