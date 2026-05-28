package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irisperez.tfg.encuentraformacion.exception.AsistenteRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("GroqClient")
class GroqClientTest {

    private GroqClient client;
    private HttpClient httpClientMock;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<Map<String, Object>> MESSAGES =
        List.of(Map.of("role", "user", "content", "hola"));
    private static final List<Map<String, Object>> NO_TOOLS = List.of();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        client = new GroqClient(mapper);
        setField(client, "apiKey", "test-groq-key");
        setField(client, "baseUrl", "https://api.groq.com/openai/v1");
        setField(client, "model", "llama-3.3-70b-versatile");
        setField(client, "timeoutSegundos", 30);
        setField(client, "maxTokens", 500);

        httpClientMock = mock(HttpClient.class);
        client.setHttpClient(httpClientMock);
    }

    @Test
    @DisplayName("respuesta 200 válida devuelve el nodo message")
    @SuppressWarnings("unchecked")
    void respuesta200_devuelveMessage() throws Exception {
        String responseBody = """
            {"model":"llama-3.3-70b-versatile","choices":[{"message":{"role":"assistant","content":"Hola!"}}]}
            """;
        doReturn(mockHttpResponse(200, responseBody)).when(httpClientMock).send(any(), any());

        JsonNode result = client.llamar(MESSAGES, NO_TOOLS);

        assertThat(result.path("content").asText()).isEqualTo("Hola!");
    }

    @Test
    @DisplayName("respuesta 503 lanza SERVICE_UNAVAILABLE")
    @SuppressWarnings("unchecked")
    void respuesta503_lanzaServiceUnavailable() throws Exception {
        doReturn(mockHttpResponse(503, "{\"error\":\"down\"}")).when(httpClientMock).send(any(), any());

        assertThatThrownBy(() -> client.llamar(MESSAGES, NO_TOOLS))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("503");
    }

    @Test
    @DisplayName("429 lanza AsistenteRateLimitException con retryAfter del header")
    @SuppressWarnings("unchecked")
    void rateLimit_lanzaAsistenteRateLimitException() throws Exception {
        doReturn(mockHttpResponse(429, "{}", Map.of("Retry-After", "30")))
            .when(httpClientMock).send(any(), any());

        assertThatThrownBy(() -> client.llamar(MESSAGES, NO_TOOLS))
            .isInstanceOf(AsistenteRateLimitException.class)
            .satisfies(ex -> assertThat(((AsistenteRateLimitException) ex).getRetryAfter()).isEqualTo(30));

        verify(httpClientMock, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("implementa LlmClient")
    void implementaLlmClient() {
        assertThat(client).isInstanceOf(LlmClient.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockHttpResponse(int status, String body) {
        return mockHttpResponse(status, body, Map.of());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockHttpResponse(int status, String body,
                                                   Map<String, String> headers) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        HttpHeaders httpHeaders = HttpHeaders.of(
            headers.entrySet().stream().collect(
                java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> List.of(e.getValue())
                )
            ),
            (k, v) -> true
        );
        when(response.headers()).thenReturn(httpHeaders);
        return response;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
