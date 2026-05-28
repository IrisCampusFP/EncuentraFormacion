package com.irisperez.tfg.encuentraformacion.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irisperez.tfg.encuentraformacion.dto.auth.LoginRequestDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static java.net.http.HttpResponse.BodyHandlers;
import static java.net.http.HttpRequest.BodyPublishers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
        usuarioRepository.deleteAll();

        Usuario usuario = new Usuario();
        usuario.setEmail("admin@test.com");
        usuario.setUsername("admin");
        usuario.setNombre("Admin");
        usuario.setApellidos("Test");
        usuario.setPassword(passwordEncoder.encode("password123"));
        usuario.setActivo(true);
        usuario.setRoles(new ArrayList<>());
        usuarioRepository.saveAndFlush(usuario);
    }

    @AfterEach
    void tearDown() {
        usuarioRepository.deleteAll();
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @DisplayName("acceso a endpoint protegido sin token retorna 401 o 403")
    void accesoSinToken_Devuelve401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBaseUrl() + "/centros"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isIn(401, 403);
    }

    @Test
    @DisplayName("login con credenciales incorrectas retorna 401")
    void loginInvalido_Devuelve401() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("admin@test.com");
        req.setPassword("MAL_PASSWORD");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(req)))
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("login con credenciales correctas retorna 200 y establece la cookie JWT")
    void loginValido_Devuelve200YCookie() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("admin@test.com");
        req.setPassword("password123");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(req)))
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        // Valida iterativamente que la cookie se envíe como 'HttpOnly' por seguridad contra ataques XSS
        List<String> cookies = response.headers().allValues("Set-Cookie");
        boolean hasJwtCookie = cookies.stream()
                .anyMatch(cookie -> cookie.contains("jwt_token") && cookie.contains("HttpOnly"));

        assertThat(hasJwtCookie).isTrue();
    }

    @Test
    @DisplayName("peticiones masivas a login superan el limite y retornan 429 Too Many Requests")
    void loginMasivo_SuperaRateLimit_Devuelve429() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("admin@test.com");
        req.setPassword("password123");
        String payload = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "192.168.1.100") // IP simulada para aislar este test de rate limit de los demás
                .POST(BodyPublishers.ofString(payload))
                .build();

        int maxIntentosPermitidos = 10;
        int statusFinal = 200;

        // Lanzamos más del límite de peticiones estipulado en RateLimitingFilter (10 por minuto)
        for (int i = 0; i < maxIntentosPermitidos + 2; i++) {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                statusFinal = 429;
                break;
            }
        }

        assertThat(statusFinal).isEqualTo(429);
    }
}
