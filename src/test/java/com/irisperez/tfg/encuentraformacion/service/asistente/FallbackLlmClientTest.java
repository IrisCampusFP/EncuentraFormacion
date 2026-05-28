package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irisperez.tfg.encuentraformacion.exception.AsistenteRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FallbackLlmClient")
class FallbackLlmClientTest {

    @Mock private GeminiClient gemini;
    @Mock private GroqClient groq;

    @InjectMocks private FallbackLlmClient fallback;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final List<Map<String, Object>> MESSAGES =
        List.of(Map.of("role", "user", "content", "hola"));
    private static final List<Map<String, Object>> NO_TOOLS = List.of();

    private ObjectNode respuestaOk;

    @BeforeEach
    void setUp() {
        respuestaOk = mapper.createObjectNode();
        respuestaOk.put("content", "Respuesta correcta");
    }

    @Test
    @DisplayName("usa Gemini cuando responde correctamente")
    void usaGeminiCuandoRespondeCorrectamente() {
        when(gemini.llamar(any(), any())).thenReturn(respuestaOk);

        var result = fallback.llamar(MESSAGES, NO_TOOLS);

        assertThat(result.path("content").asText()).isEqualTo("Respuesta correcta");
        verify(gemini).llamar(any(), any());
        verifyNoInteractions(groq);
    }

    @Test
    @DisplayName("usa Groq cuando Gemini devuelve rate limit")
    void usaGroqCuandoGeminiDevuelveRateLimit() {
        ObjectNode respuestaGroq = mapper.createObjectNode();
        respuestaGroq.put("content", "Respuesta de Groq");

        when(gemini.llamar(any(), any())).thenThrow(new AsistenteRateLimitException(60));
        when(groq.llamar(any(), any())).thenReturn(respuestaGroq);

        var result = fallback.llamar(MESSAGES, NO_TOOLS);

        assertThat(result.path("content").asText()).isEqualTo("Respuesta de Groq");
        verify(gemini).llamar(any(), any());
        verify(groq).llamar(any(), any());
    }

    @Test
    @DisplayName("propaga AsistenteRateLimitException si Groq también está limitado")
    void propagaExcepcionSiGroqTambienEstaLimitado() {
        when(gemini.llamar(any(), any())).thenThrow(new AsistenteRateLimitException(60));
        when(groq.llamar(any(), any())).thenThrow(new AsistenteRateLimitException(30));

        assertThatThrownBy(() -> fallback.llamar(MESSAGES, NO_TOOLS))
            .isInstanceOf(AsistenteRateLimitException.class)
            .satisfies(ex -> assertThat(((AsistenteRateLimitException) ex).getRetryAfter()).isEqualTo(30));
    }

    @Test
    @DisplayName("Groq recibe exactamente los mismos mensajes y tools que Gemini recibió")
    void groqRecibeMismosArgumentos() {
        var tools = List.of(Map.<String, Object>of("type", "function"));
        when(gemini.llamar(any(), any())).thenThrow(new AsistenteRateLimitException(60));
        when(groq.llamar(any(), any())).thenReturn(respuestaOk);

        fallback.llamar(MESSAGES, tools);

        verify(groq).llamar(MESSAGES, tools);
    }

    @Test
    @DisplayName("tras rate limit de Gemini, la siguiente petición va directamente a Groq sin llamar a Gemini")
    void cooldownEliminaLlamadaAGeminiTrasRateLimit() {
        ObjectNode respuestaGroq = mapper.createObjectNode();
        respuestaGroq.put("content", "Groq responde");

        // Primera petición: Gemini da 429 → cooldown activado
        when(gemini.llamar(any(), any())).thenThrow(new AsistenteRateLimitException(60));
        when(groq.llamar(any(), any())).thenReturn(respuestaGroq);
        fallback.llamar(MESSAGES, NO_TOOLS);

        // Segunda petición: debe ir directo a Groq sin tocar Gemini
        fallback.llamar(MESSAGES, NO_TOOLS);

        // Gemini solo fue llamado 1 vez (la primera); la segunda fue Groq directamente
        verify(gemini, times(1)).llamar(any(), any());
        verify(groq, times(2)).llamar(any(), any());
    }
}
