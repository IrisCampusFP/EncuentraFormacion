package com.irisperez.tfg.encuentraformacion.controller.auth;

import com.irisperez.tfg.encuentraformacion.dto.auth.AuthResponseDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.LoginRequestDTO;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.security.JwtService;
import com.irisperez.tfg.encuentraformacion.security.SecurityAuditService;
import com.irisperez.tfg.encuentraformacion.security.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador encargado de la autenticación de usuarios.
 * Proporciona el endpoint para iniciar sesión y obtener el token JWT.
 * Gestiona el inicio y cierre de sesión estableciendo la cookie JWT desde el backend
 * con las flags HttpOnly, Secure y SameSite=Strict para proteger contra XSS.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityAuditService auditService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.cookie.secure}")
    private boolean cookieSecure;

    /**
     * Autentica al usuario y establece la cookie JWT en la respuesta HTTP.
     * El token NO se devuelve en el cuerpo de la respuesta; reside exclusivamente
     * en la cookie HttpOnly para evitar que sea accesible desde JavaScript.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request, HttpServletRequest httpRequest, HttpServletResponse response) {
        Authentication authentication;
        try {
            // Autentica al usuario usando el AuthenticationManager de Spring Security
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            // Registro de auditoría para monitorización de intentos de autenticación
            auditService.loginFallido(request.getEmail(), httpRequest.getRemoteAddr());
            throw e;
        }

        // Se obtienen los datos del usuario con el metodo de Spring Security getPrincipal()
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Si el usuario no es válido, se devuelve un error 401 Unauthorized
        if (userDetails == null) {
            auditService.loginFallido(request.getEmail(), httpRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Registro de auditoría de autenticación correcta
        auditService.loginExitoso(request.getEmail(), httpRequest.getRemoteAddr());

        // Se genera el token JWT
        String token = jwtService.generarTokenBasico(userDetails);

        // Se establece la cookie JWT con flags de seguridad desde el backend.
        // HttpOnly impide el acceso desde JavaScript (protección XSS).
        // Secure garantiza que solo se envíe por HTTPS.
        // SameSite=Strict evita envíos cross-site (protección CSRF).
        ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(jwtExpiration / 1000) // duración de la cookie desde que se crea en segundos
                .build();

        response.addHeader("Set-Cookie", jwtCookie.toString());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .email(userDetails.getEmail())
                .roles(roles)
                .build());
    }

    /**
     * Cierra la sesión invalidando la cookie JWT y bloqueando el token para prevenir reutilización (Session Hijacking).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = null;

        // Intentar sacar el token del Authorization header o de la cookie
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // Invalida el token almacenándolo de forma persistente para prevenir ataques de secuestro de sesión
        if (token != null) {
            tokenBlacklistService.blacklistarToken(token);
        }

        // Registro de auditoría de fin de sesión
        // Si hay token se intenta extraer el email, sino se registra con email desconocido
        String email = "desconocido";
        if (token != null) {
            try { email = jwtService.extraerEmail(token); } catch (Exception ignored) {}
        }
        auditService.logout(email, request.getRemoteAddr());

        ResponseCookie cookieVacia = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookieVacia.toString());
        return ResponseEntity.ok().build();
    }
}
