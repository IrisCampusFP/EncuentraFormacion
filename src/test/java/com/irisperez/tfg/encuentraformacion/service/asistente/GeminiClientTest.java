package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irisperez.tfg.encuentraformacion.exception.AsistenteRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

@DisplayName("GeminiClient")
class GeminiClientTest {

    @Nested
    @DisplayName("Respuestas HTTP")
    class RespuestasHttp {

        private GeminiClient client;
        private HttpClient httpClientMock;
        private final ObjectMapper mapper = new ObjectMapper();

        private static final List<Map<String, Object>> MESSAGES =
            List.of(Map.of("role", "user", "content", "hola"));
        private static final List<Map<String, Object>> NO_TOOLS = List.of();

        @BeforeEach
        void setUp() throws Exception {
            client = new GeminiClient(mapper);
            setField(client, "apiKey", "test-key");
            setField(client, "baseUrl", "https://generativelanguage.googleapis.com/v1beta");
            setField(client, "model", "gemini-2.0-flash");
            setField(client, "timeoutSegundos", 30);
            setField(client, "maxTokens", 500);

            httpClientMock = mock(HttpClient.class);
            client.setHttpClient(httpClientMock);
        }

        @Test
        @DisplayName("200 con texto devuelve nodo con content")
        @SuppressWarnings("unchecked")
        void respuesta200ConTexto_devuelveContent() throws Exception {
            String responseBody = """
                {
                  "candidates": [{
                    "content": {
                      "role": "model",
                      "parts": [{"text": "Hola!"}]
                    }
                  }]
                }
                """;
            doReturn(mockHttpResponse(200, responseBody)).when(httpClientMock).send(any(), any());

            JsonNode result = client.llamar(MESSAGES, NO_TOOLS);

            assertThat(result.path("content").asText()).isEqualTo("Hola!");
        }

        @Test
        @DisplayName("200 con functionCall devuelve tool_calls en formato OpenAI")
        @SuppressWarnings("unchecked")
        void respuesta200ConFunctionCall_devuelveToolCallsFormatoOpenAI() throws Exception {
            String responseBody = """
                {
                  "candidates": [{
                    "content": {
                      "role": "model",
                      "parts": [{
                        "functionCall": {
                          "name": "buscarFormaciones",
                          "args": {"nombre": "programacion"}
                        }
                      }]
                    }
                  }]
                }
                """;
            doReturn(mockHttpResponse(200, responseBody)).when(httpClientMock).send(any(), any());

            JsonNode result = client.llamar(MESSAGES, NO_TOOLS);

            assertThat(result.has("tool_calls")).isTrue();
            assertThat(result.path("tool_calls").isArray()).isTrue();
            assertThat(result.path("tool_calls").get(0).path("function").path("name").asText())
                .isEqualTo("buscarFormaciones");
        }

        @Test
        @DisplayName("503 lanza SERVICE_UNAVAILABLE")
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
            doReturn(mockHttpResponse(429, "{}", Map.of("Retry-After", "60")))
                .when(httpClientMock).send(any(), any());

            assertThatThrownBy(() -> client.llamar(MESSAGES, NO_TOOLS))
                .isInstanceOf(AsistenteRateLimitException.class)
                .satisfies(ex -> assertThat(((AsistenteRateLimitException) ex).getRetryAfter()).isEqualTo(60));

            verify(httpClientMock, times(1)).send(any(), any());
        }

        @Test
        @DisplayName("429 sin header Retry-After usa 60s por defecto")
        @SuppressWarnings("unchecked")
        void rateLimit_sinHeader_usa60sPorDefecto() throws Exception {
            doReturn(mockHttpResponse(429, "{}")).when(httpClientMock).send(any(), any());

            assertThatThrownBy(() -> client.llamar(MESSAGES, NO_TOOLS))
                .isInstanceOf(AsistenteRateLimitException.class)
                .satisfies(ex -> assertThat(((AsistenteRateLimitException) ex).getRetryAfter()).isEqualTo(60));
        }

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

    @Nested
    @DisplayName("AsistenteRateLimitException")
    class RateLimitExceptionTests {

        @Test
        @DisplayName("lleva retryAfter y lo incluye en el mensaje")
        void tieneRetryAfterYMensaje() {
            var ex = new AsistenteRateLimitException(30);

            assertThat(ex.getRetryAfter()).isEqualTo(30);
            assertThat(ex.getReason()).contains("30");
        }
    }

    @Nested
    @DisplayName("calcularRetryAfter")
    class CalcularRetryAfterTests {

        private GeminiClient client;

        @BeforeEach
        void setUp() throws Exception {
            client = new GeminiClient(new ObjectMapper());
        }

        @Test
        @DisplayName("cuota diaria (PerDay en quotaId) devuelve segundos hasta medianoche Pacific")
        void cuotaDiariaDevuelveSegundosHastaMedianoche() {
            String body = """
                {
                  "error": {
                    "code": 429,
                    "details": [
                      {
                        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
                        "violations": [
                          {
                            "quotaId": "GenerateRequestsPerDayPerProjectPerModel-FreeTier"
                          }
                        ]
                      },
                      {
                        "@type": "type.googleapis.com/google.rpc.RetryInfo",
                        "retryDelay": "34s"
                      }
                    ]
                  }
                }
                """;
            int segundos = client.calcularRetryAfter(body);
            // PerDay tiene prioridad sobre retryDelay (34s). Resultado: segundos hasta medianoche Pacific
            assertThat(segundos).isGreaterThan(34).isLessThanOrEqualTo(86400);
        }

        @Test
        @DisplayName("rate limit de RPM sin cuota diaria usa retryDelay de RetryInfo")
        void rpmSinCuotaDiariaUsaRetryDelay() {
            String body = """
                {
                  "error": {
                    "code": 429,
                    "details": [
                      {
                        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
                        "violations": [
                          {
                            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier"
                          }
                        ]
                      },
                      {
                        "@type": "type.googleapis.com/google.rpc.RetryInfo",
                        "retryDelay": "42s"
                      }
                    ]
                  }
                }
                """;
            assertThat(client.calcularRetryAfter(body)).isEqualTo(42);
        }

        @Test
        @DisplayName("body sin información conocida devuelve 60s por defecto")
        void bodyDesconocidoDevuelve60() {
            assertThat(client.calcularRetryAfter("{\"error\":{\"code\":429}}")).isEqualTo(60);
        }
    }
}
