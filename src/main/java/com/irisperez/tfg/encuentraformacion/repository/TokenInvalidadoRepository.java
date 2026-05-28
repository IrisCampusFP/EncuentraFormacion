package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.TokenInvalidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;

/**
 * Repositorio para la lista negra de tokens JWT, previniendo el uso de tokens comprometidos o caducados (Cryptographic/Auth Failures).
 */
public interface TokenInvalidadoRepository extends JpaRepository<TokenInvalidado, Long> {

    /**
     * Comprobación rápida gracias al índice idx_tokens_invalidados_hash.
     */
    boolean existsByTokenHash(String tokenHash);

    /**
     * Limpieza nocturna de tokens expirados para evitar crecimiento indefinido de la tabla.
     */
    @Modifying
    @Query("DELETE FROM TokenInvalidado t WHERE t.fechaExpiracion < :ahora")
    int deleteByFechaExpiracionBefore(LocalDateTime ahora);
}
