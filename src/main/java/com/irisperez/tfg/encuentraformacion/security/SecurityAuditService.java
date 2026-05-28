package com.irisperez.tfg.encuentraformacion.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditService {

    // Logger separado — se enviará a un archivo distinto (configurado en logback-spring.xml)
    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void loginExitoso(String email, String ip) {
        AUDIT.info("LOGIN_SUCCESS | email={} | ip={}", LogSanitizer.sanitize(email), LogSanitizer.sanitize(ip));
    }

    public void loginFallido(String email, String ip) {
        AUDIT.warn("LOGIN_FAILURE | email={} | ip={}", LogSanitizer.sanitize(email), LogSanitizer.sanitize(ip));
    }

    public void logout(String email, String ip) {
        AUDIT.info("LOGOUT | email={} | ip={}", LogSanitizer.sanitize(email), LogSanitizer.sanitize(ip));
    }

    public void accesoDenegado(String recurso, String ip) {
        AUDIT.warn("ACCESS_DENIED | recurso={} | ip={}", LogSanitizer.sanitize(recurso), LogSanitizer.sanitize(ip));
    }

    public void rateLimitSuperado(String endpoint, String ip) {
        AUDIT.warn("RATE_LIMIT_EXCEEDED | endpoint={} | ip={}", LogSanitizer.sanitize(endpoint), LogSanitizer.sanitize(ip));
    }

    public void tokenInvalidado(String ip) {
        AUDIT.warn("TOKEN_BLACKLISTED_USED | ip={}", LogSanitizer.sanitize(ip));
    }
}
