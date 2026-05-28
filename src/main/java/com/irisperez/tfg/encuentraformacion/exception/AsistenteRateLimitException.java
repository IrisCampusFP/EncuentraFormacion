package com.irisperez.tfg.encuentraformacion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AsistenteRateLimitException extends ResponseStatusException {

    private final int retryAfter;

    public AsistenteRateLimitException(int retryAfter) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
            "El asistente está muy ocupado en este momento. Inténtalo de nuevo en unos minutos.");
        this.retryAfter = retryAfter;
    }

    public int getRetryAfter() { return retryAfter; }
}
