package com.irisperez.tfg.encuentraformacion.security;

/**
 * Utilidad de sanitización para logs de seguridad.
 * Elimina caracteres de control (\n \r \t) que podrían usarse para falsificar
 * entradas de log (log injection — OWASP A03/A09), y trunca a MAX_LENGTH
 * para evitar que inputs masivos saturen los logs.
 *
 * Uso: llamar a LogSanitizer.sanitize(valor) antes de pasar cualquier
 * dato controlado por el usuario a log.warn() o log.error().
 */
public final class LogSanitizer {

    private static final int MAX_LENGTH = 200;

    private LogSanitizer() {}

    /**
     * Elimina caracteres de control (\n \r \t) y trunca a MAX_LENGTH caracteres.
     * Usar con cualquier valor de usuario antes de pasarlo a log.warn/log.error.
     */
    public static String sanitize(String value) {
        if (value == null) return "null";
        String limpio = value.replaceAll("[\n\r\t]", "_");
        return limpio.length() > MAX_LENGTH ? limpio.substring(0, MAX_LENGTH) + "…" : limpio;
    }
}
