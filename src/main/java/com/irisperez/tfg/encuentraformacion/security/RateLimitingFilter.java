package com.irisperez.tfg.encuentraformacion.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Filtro de rate limiting para los endpoints de login y registro.
 *
 * Login:    5 intentos por IP en ventana de 1 minuto (Bucket4j).
 * Registro: 3 intentos por IP en ventana de 1 hora   (Bucket4j).
 *
 * Rate limit en /registro/** para mitigar ataques de fuerza bruta y denegación de servicio (DoS).
 * Anti-spoofing de X-Forwarded-For: validación estricta de origen para prevenir suplantación de IP (IP Spoofing).
 *           proviene de una IP proxy de confianza (configurable via RATE_LIMIT_TRUSTED_PROXIES).
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";
    private static final String REGISTRO_PATH_PREFIX = "/registro";

    private static final int MAX_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(1);

    private static final int MAX_INTENTOS_REGISTRO = 3;
    private static final Duration VENTANA_REGISTRO = Duration.ofHours(1);

    // Mapa IP → Bucket para login
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Mapa IP -> Bucket para rate limiting de registro
    private final ConcurrentHashMap<String, Bucket> bucketsRegistro = new ConcurrentHashMap<>();

    // Proxies de confianza para validación de cabeceras
    @Value("${rate-limiting.trusted-proxies:}")
    private String trustedProxiesConfig;

    private Set<String> trustedProxies = Set.of();

    @PostConstruct
    void init() {
        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            trustedProxies = Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());
        }
    }

    // Servicio de auditoría de seguridad
    private final SecurityAuditService auditService;

    public RateLimitingFilter(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        boolean esLogin = LOGIN_PATH.equals(path);
        boolean esRegistro = path != null && path.startsWith(REGISTRO_PATH_PREFIX);

        // Solo aplicamos el filtro a login y registro
        if (!esLogin && !esRegistro) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = obtenerIp(request);

        if (esLogin) {
            Bucket bucket = buckets.computeIfAbsent(ip, this::crearBucket);
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                log.warn("Rate limit LOGIN superado para la IP [{}]", LogSanitizer.sanitize(ip));
                auditService.rateLimitSuperado(path, ip); // Registro de auditoría
                responderRateLimit(response, "Demasiados intentos de inicio de sesión. Espera 1 minuto antes de volver a intentarlo.");
            }
            return;
        }

        // Validación de endpoint para rate limiting de registro
        Bucket bucketReg = bucketsRegistro.computeIfAbsent(ip, this::crearBucketRegistro);
        if (bucketReg.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit REGISTRO superado para la IP [{}]", LogSanitizer.sanitize(ip));
            auditService.rateLimitSuperado(path, ip); // Registro de auditoría
            responderRateLimit(response, "Demasiados intentos de registro. Espera 1 hora antes de volver a intentarlo.");
        }
    }

    /**
     * Crea un Bucket de login: 5 intentos por minuto.
     */
    private Bucket crearBucket(String ip) {
        Bandwidth limite = Bandwidth.builder()
                .capacity(MAX_INTENTOS)
                .refillGreedy(MAX_INTENTOS, VENTANA)
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    /**
     * Crea un Bucket de control de solicitudes de registro (mitigación anti-abuso).
     */
    private Bucket crearBucketRegistro(String ip) {
        Bandwidth limite = Bandwidth.builder()
                .capacity(MAX_INTENTOS_REGISTRO)
                .refillGreedy(MAX_INTENTOS_REGISTRO, VENTANA_REGISTRO)
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    /**
     * Obtiene la IP real del cliente de forma segura.
     * Solo confía en X-Forwarded-For si la petición proviene de un proxy de confianza.
     * Sin proxies configurados (desarrollo local), siempre usa RemoteAddr.
     */
    String obtenerIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    /**
     * Escribe una respuesta HTTP 429 Too Many Requests con cuerpo JSON.
     */
    private void responderRateLimit(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"errorMsg\":\"" + mensaje + "\"}");
    }
}
