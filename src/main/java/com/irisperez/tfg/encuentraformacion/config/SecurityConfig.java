package com.irisperez.tfg.encuentraformacion.config;

import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.security.JwtAccessDeniedHandler;
import com.irisperez.tfg.encuentraformacion.security.JwtAuthenticationEntryPoint;
import com.irisperez.tfg.encuentraformacion.security.JwtAuthenticationFilter;
import com.irisperez.tfg.encuentraformacion.security.RateLimitingFilter;
import com.irisperez.tfg.encuentraformacion.service.auth.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
/**
 * Configuración principal de Spring Security usando JWT (Stateless).
 * Define autenticación y autorización stateless, qué rutas requieren
 * autenticación o roles específicos, maneja errores de autenticación
 * y configura el filtro JWT (que valida el token en cada petición).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita @PreAuthorize y @PostAuthorize en los controladores
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final RateLimitingFilter rateLimitingFilter;

    // Orígenes permitidos para mitigar configuraciones incorrectas de CORS (Security Misconfiguration)
    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    // ENCODER DE CONTRASEÑAS (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
 
    // AUTHENTICATION MANAGER
    
    /*  Se inyecta el CustomUserDetailsService y el PasswordEncoder para que Spring Security
        sepa cómo cargar y verificar las credenciales durante el login. */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
                builder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder());
                return builder.build();
    }

    // SECURITY FILTER CHAIN (cadena de filtros de seguridad)
    
    /*
     * Define qué rutas están protegidas y quién puede acceder a ellas.
     *
     * - CSRF desactivado: la autenticación se delega al JWT (HttpOnly cookie).
     * - Sesiones en modo STATELESS: Spring Security no crea ni usa sesión HTTP.
     * - Rate limiting aplicado antes del filtro JWT para bloquear ataques de fuerza bruta.
     * - hasRole("X") equivale a hasAuthority("ROLE_X"), Spring añade el prefijo.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF desactivado: la autenticación se delega al JWT (HttpOnly cookie).
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Configuración estricta de orígenes compartidos (CORS)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jwtAuthEntryPoint)   // Captura errores de autenticación (401 Unauthorized)
                        .accessDeniedHandler(jwtAccessDeniedHandler)   // Captura errores de autorización (403 Forbidden)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Sesiones en modo STATELESS: Spring Security no crea ni usa sesión HTTP.
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class) // Rate limiting aplicado antes del filtro JWT para bloquear ataques de fuerza bruta.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // Filtro JWT (que valida el token en cada petición).
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // Permite embedding solo desde el mismo origen (necesario para la vista previa de perfil).
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)) // Protección contra ataques XSS.
                        .contentTypeOptions(Customizer.withDefaults()) // Activa X-Content-Type-Options para evitar ataques de inyección MIME (sniffing).
                        .cacheControl(Customizer.withDefaults()) // Desactiva el caché para evitar que el navegador almacene contenido sensible.
                        .httpStrictTransportSecurity(Customizer.withDefaults()) // Habilita HSTS para forzar el uso de HTTPS.
                        // Content-Security-Policy (CSP) para mitigar Cross-Site Scripting (XSS)
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self' https://cdn.jsdelivr.net; " +
                            "style-src 'self' https://cdn.jsdelivr.net; " +
                            "font-src 'self' https://cdn.jsdelivr.net data:; " +
                            "img-src 'self' data: blob:; " +
                            "connect-src 'self' ws://localhost:8080 wss://localhost:8443; " +
                            "frame-ancestors 'self'; " +
                            "base-uri 'self'; " +
                            "form-action 'self'"
                        ))
                )
                .authorizeHttpRequests(auth -> auth

                        // Recursos estáticos
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()


                        // VISTAS (HTML)

                        // 1. Públicas

                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/vistas/auth/**",
                                "/vistas/publico/detalle-formacion.html",
                                "/vistas/publico/perfil-centro.html",
                                "/vistas/publico/buscar-centros.html",
                                "/vistas/publico/aviso-legal.html",
                                "/vistas/publico/contacto.html",
                                "/vistas/publico/politica-privacidad.html",
                                "/vistas/estudiante/asistente-ia.html"
                        ).permitAll()

                        // 2. Protegidas por rol

                        // Vistas ADMIN
                        .requestMatchers("/vistas/admin/**").hasRole(RolNombre.ADMIN.name())

                        // Vistas GESTOR_CENTRO
                        .requestMatchers("/vistas/gestor/**").hasRole(RolNombre.GESTOR_CENTRO.name())

                        // Vistas ESTUDIANTE (asistente-ia.html ya está declarada pública arriba)
                        .requestMatchers("/vistas/estudiante/**").hasRole(RolNombre.ESTUDIANTE.name())

                        // 3. Vistas que requieren AUTENTICACIÓN
                        .requestMatchers(
                                "/vistas/comun/estado-solicitud.html",
                                "/vistas/comun/perfil.html",
                                "/vistas/comun/solicitar-gestion-centro.html"
                        ).authenticated()


                        // ENDPOINTS (API)

                        // Públicos
                        .requestMatchers(
                                "/auth/**",
                                "/registro/**",
                                "/check-email-unique",
                                "/check-username-unique",
                                "/centros/existe",
                                "/centros/*/perfil",
                                "/centros/*/formaciones",
                                "/centros/buscar",
                                "/formaciones/**",
                                "/grado-estudios",
                                "/tipo-estudios",
                                "/provincias",
                                "/comunidades-autonomas",
                                "/error/**"
                        ).permitAll()

                        // Solo GESTOR_CENTRO — notificaciones propias
                        .requestMatchers("/notificaciones/gestor/**").hasRole(RolNombre.GESTOR_CENTRO.name())

                        // Solo ESTUDIANTE
                        .requestMatchers(
                            "/solicitudes-formacion/**",
                            "/valoraciones",
                            "/valoraciones/**",
                            "/favoritos/**",
                            "/chat/**",
                            "/notificaciones/**",
                            "/api/asistente/**"
                        ).hasRole(RolNombre.ESTUDIANTE.name())

                        // Solo ADMIN
                        .requestMatchers(
                                "/usuarios/**",
                                "/centros/**",
                                "/roles/**"
                        ).hasRole(RolNombre.ADMIN.name())

                        // WebSocket handshake — la autenticación la gestiona WebSocketAuthInterceptor
                        .requestMatchers("/ws/**").permitAll()

                        // Requieren autenticación
                        .requestMatchers("/perfil", "/perfil/password", "/solicitudes-gestion/**").authenticated()

                        // Solo GESTOR_CENTRO
                        .requestMatchers("/gestor/**").hasRole(RolNombre.GESTOR_CENTRO.name())

                        // Catch-all (cualquier otra ruta requiere autenticación)
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * Configura políticas CORS estrictas leyendo los orígenes de confianza del entorno.
     * En producción, configurar la variable de entorno CORS_ALLOWED_ORIGINS con el dominio real.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(corsAllowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept"));
        config.setAllowCredentials(true); // Necesario para que las cookies se envíen en peticiones CORS
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}