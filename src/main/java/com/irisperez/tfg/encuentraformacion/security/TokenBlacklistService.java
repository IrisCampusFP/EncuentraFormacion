package com.irisperez.tfg.encuentraformacion.security;

import com.irisperez.tfg.encuentraformacion.model.entity.TokenInvalidado;
import com.irisperez.tfg.encuentraformacion.repository.TokenInvalidadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

/**
 * Servicio para gestionar la lista negra de tokens JWT e impedir ataques de suplantación de identidad (Authentication Failures).
 * Se encarga de invalidar tokens en el logout y comprobar si están en la blacklist
 * durante la autenticación. También realiza la limpieza programada de tokens expirados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final TokenInvalidadoRepository tokenInvalidadoRepository;
    private final JwtService jwtService;

    /**
     * Añade un token a la lista negra. Extrae su fecha de expiración real y
     * almacena solo el hash SHA-256.
     */
    @Transactional
    public void blacklistarToken(String token) {
        try {
            String hash = hashToken(token);

            // Si ya está en la lista negra, no hacemos nada
            if (tokenInvalidadoRepository.existsByTokenHash(hash)) {
                return;
            }

            Date expiracionDate = jwtService.extraerFechaExpiracion(token);
            LocalDateTime expiracion = LocalDateTime.ofInstant(expiracionDate.toInstant(), ZoneId.systemDefault());

            TokenInvalidado tokenInvalidado = new TokenInvalidado(hash, LocalDateTime.now(), expiracion);
            tokenInvalidadoRepository.save(tokenInvalidado);

            log.info("Token añadido a la lista negra. Expira el: {}", expiracion);
        } catch (Exception e) {
            log.error("Error al blacklistar token (posiblemente malformado o expirado): {}", e.getMessage());
            // Si el token ya es inválido o está expirado para JJWT, no hace falta blacklistarlo
        }
    }

    /**
     * Verifica de forma rápida (índice BD) si el token está en la lista negra.
     */
    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String token) {
        String hash = hashToken(token);
        return tokenInvalidadoRepository.existsByTokenHash(hash);
    }

    /**
     * Genera el hash SHA-256 en Base64 del token. Nunca se guarda el token en claro.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible", e);
        }
    }

    /**
     * Limpieza programada de tokens que ya superaron su fecha de caducidad,
     * dado que de todos modos serían rechazados por JwtService (evita crecimiento infinito).
     * Se ejecuta todos los días a las 03:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limpiarTokensExpirados() {
        log.info("Iniciando limpieza programada de la blacklist de tokens...");
        int eliminados = tokenInvalidadoRepository.deleteByFechaExpiracionBefore(LocalDateTime.now());
        log.info("Limpieza finalizada. {} tokens expirados eliminados de la lista negra.", eliminados);
    }
}
