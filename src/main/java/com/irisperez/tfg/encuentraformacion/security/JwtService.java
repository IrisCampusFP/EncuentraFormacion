package com.irisperez.tfg.encuentraformacion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio encargado de la gestión de JSON Web Tokens (JWT).
 * Proporciona métodos para generar, validar y extraer información de los tokens.
 * Utiliza la librería JJWT versión 0.12.x.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String key;

    @Value("${jwt.expiration}")
    private long duracion;


    // MÉTODOS DE EXTRACCIÓN

    // Extrae el email (subject) del token
    public String extraerEmail(String token) {
        Claims claims = extraerTodosLosClaims(token);
        return claims.getSubject();
    }

    // Extrae la fecha de caducidad del token (público para validación en lista negra)
    public Date extraerFechaExpiracion(String token) {
        Claims claims = extraerTodosLosClaims(token);
        return claims.getExpiration();
    }

    // Metodo interno para abrir el token y leer todos sus datos (Claims)
    private Claims extraerTodosLosClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    // MÉTODOS DE GENERACIÓN

    // Genera un token básico para un usuario
    public String generarTokenBasico(UserDetails userDetails) {
        // Llama al metodo que genera el token con datos adicionales pero no le pasa ningún dato adicional
        return generarTokenConClaims(new HashMap<>(), userDetails);
    }

    // Genera un token con datos adicionales (claims)
    public String generarTokenConClaims(Map<String, Object> claims, UserDetails userDetails) {
        return Jwts
                .builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + duracion))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }


    // MÉTODOS DE VALIDACIÓN

    // Verifica que el token sea válido para el usuario y no haya caducado
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String emailToken = extraerEmail(token);
        return (emailToken.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Comprueba si el token ha superado su fecha de caducidad
    private boolean isTokenExpired(String token) {
        return extraerFechaExpiracion(token).before(new Date());
    }

    // Obtiene la clave secreta en el formato que necesita la librería JJWT
    private SecretKey getSignInKey() {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
