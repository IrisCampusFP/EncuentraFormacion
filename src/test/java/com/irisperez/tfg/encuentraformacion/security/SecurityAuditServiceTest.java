package com.irisperez.tfg.encuentraformacion.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("SecurityAuditService")
class SecurityAuditServiceTest {

    private final SecurityAuditService auditService = new SecurityAuditService();

    @Test
    @DisplayName("loginExitoso no lanza excepciones con entradas válidas")
    void debeLoguearLoginExitoso() {
        assertThatNoException().isThrownBy(() ->
            auditService.loginExitoso("user@test.com", "127.0.0.1")
        );
    }

    @Test
    @DisplayName("loginFallido no lanza excepciones con input malicioso")
    void debeLoguearSinExcepcionConInputMalicioso() {
        assertThatNoException().isThrownBy(() ->
            auditService.loginFallido("attacker\nFAKE_LOG", "1.2.3.4")
        );
    }

    @Test
    @DisplayName("logout no lanza excepciones")
    void logout_noLanzaExcepcion() {
        assertThatNoException().isThrownBy(() ->
            auditService.logout("user@test.com", "192.168.1.1")
        );
    }

    @Test
    @DisplayName("accesoDenegado no lanza excepciones")
    void accesoDenegado_noLanzaExcepcion() {
        assertThatNoException().isThrownBy(() ->
            auditService.accesoDenegado("/api/admin/usuarios", "10.0.0.1")
        );
    }

    @Test
    @DisplayName("rateLimitSuperado no lanza excepciones")
    void rateLimitSuperado_noLanzaExcepcion() {
        assertThatNoException().isThrownBy(() ->
            auditService.rateLimitSuperado("/auth/login", "203.0.113.42")
        );
    }

    @Test
    @DisplayName("tokenInvalidado no lanza excepciones")
    void tokenInvalidado_noLanzaExcepcion() {
        assertThatNoException().isThrownBy(() ->
            auditService.tokenInvalidado("172.16.0.5")
        );
    }
}
