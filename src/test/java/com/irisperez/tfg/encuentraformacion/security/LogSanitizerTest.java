package com.irisperez.tfg.encuentraformacion.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de LogSanitizer para asegurar la prevención de inyección en logs (Log Injection).
 * Verifica que el sanitizador elimina caracteres de control y trunca cadenas largas.
 */
class LogSanitizerTest {

    @Test
    void debeEliminarSaltosDeLinea() {
        String input = "admin@test.com\nFAKE_LOG_LINE";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).doesNotContain("\n");
        assertThat(result).contains("_");
    }

    @Test
    void debeEliminarRetornoDeCarro() {
        String input = "admin@test.com\rINJECTED";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).doesNotContain("\r");
        assertThat(result).contains("_");
    }

    @Test
    void debeEliminarTabulaciones() {
        String input = "valor\tinyectado";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).doesNotContain("\t");
        assertThat(result).contains("_");
    }

    @Test
    void debeTruncarCadenasLargas() {
        String input = "a".repeat(300);
        assertThat(LogSanitizer.sanitize(input)).hasSizeLessThanOrEqualTo(202); // 200 + "…"
    }

    @Test
    void debeManejarnull() {
        assertThat(LogSanitizer.sanitize(null)).isEqualTo("null");
    }

    @Test
    void debePermitirCadenasCortas() {
        String input = "ip-192.168.1.1";
        assertThat(LogSanitizer.sanitize(input)).isEqualTo(input);
    }
}
