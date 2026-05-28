package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irisperez.tfg.encuentraformacion.exception.AsistenteRateLimitException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GroqClient implements LlmClient {

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.base-url}")
    private String baseUrl;

    @Value("${groq.model}")
    private String model;

    @Value("${groq.timeout-segundos}")
    private int timeoutSegundos;

    @Value("${groq.max-tokens}")
    private int maxTokens;

    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    public GroqClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /** Package-private para inyectar un stub en tests sin levantar contexto Spring. */
    void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public JsonNode llamar(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            return ejecutarLlamada(messages, tools);
        } catch (RateLimitException e) {
            log.warn("Groq rate limit — Retry-After: {}s", e.getRetryAfter());
            throw new AsistenteRateLimitException(e.getRetryAfter());
        }
    }

    private JsonNode ejecutarLlamada(List<Map<String, Object>> messages,
                                     List<Map<String, Object>> tools) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.7);
            body.set("messages", objectMapper.valueToTree(messages));
            if (!tools.isEmpty()) {
                body.set("tools", objectMapper.valueToTree(tools));
                body.put("tool_choice", "auto");
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(timeoutSegundos))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 429) {
                int retryAfter = response.headers().firstValue("Retry-After")
                    .map(v -> { try { return Integer.parseInt(v.trim()); } catch (NumberFormatException x) { return 60; } })
                    .orElse(60);
                log.warn("Groq HTTP 429 — Retry-After: {}s", retryAfter);
                throw new RateLimitException(retryAfter);
            }

            if (status != 200) {
                String responseBody = response.body();
                log.error("Groq respondió {} — body: {}", status,
                    responseBody.substring(0, Math.min(2000, responseBody.length())));
                if (responseBody.contains("tool_use_failed") || responseBody.contains("Failed to call a function")) {
                    throw new ToolCallFailedException();
                }
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El asistente no está disponible en este momento. Inténtalo de nuevo.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            log.info("Respuesta de Groq — modelo: {}", root.path("model").asText("desconocido"));

            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || choices.isEmpty()) {
                log.warn("Groq respuesta sin choices — body: {}",
                    response.body().substring(0, Math.min(200, response.body().length())));
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El asistente no pudo generar una respuesta. Inténtalo de nuevo.");
            }

            return choices.get(0).path("message");

        } catch (RateLimitException | ToolCallFailedException | ResponseStatusException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            log.warn("Timeout tras {}s esperando respuesta de Groq", timeoutSegundos);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                "El asistente tardó demasiado en responder. Inténtalo de nuevo.");
        } catch (Exception e) {
            log.error("Error inesperado llamando a Groq: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El asistente no está disponible en este momento. Inténtalo de nuevo.");
        }
    }

    static final class RateLimitException extends RuntimeException {
        private final int retryAfter;
        RateLimitException(int retryAfter) {
            super("Rate limit — Retry-After: " + retryAfter + "s");
            this.retryAfter = retryAfter;
        }
        int getRetryAfter() { return retryAfter; }
    }

    /** El modelo generó arguments malformados (XML, texto libre, etc.) en la tool call. */
    static final class ToolCallFailedException extends RuntimeException {
        ToolCallFailedException() { super("tool_use_failed"); }
    }
}
