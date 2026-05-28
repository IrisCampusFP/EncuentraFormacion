package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiClient implements LlmClient {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.base-url}")
    private String baseUrl;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.timeout-segundos}")
    private int timeoutSegundos;

    @Value("${gemini.max-tokens}")
    private int maxTokens;

    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    public GeminiClient(ObjectMapper objectMapper) {
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
            log.warn("Gemini rate limit — Retry-After: {}s", e.getRetryAfter());
            throw new AsistenteRateLimitException(e.getRetryAfter());
        }
    }

    private JsonNode ejecutarLlamada(List<Map<String, Object>> messages,
                                     List<Map<String, Object>> tools) {
        try {
            ObjectNode body = buildRequestBody(messages, tools);
            String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(timeoutSegundos))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 429) {
                int retryAfter = calcularRetryAfter(response.body());
                throw new RateLimitException(retryAfter);
            }

            if (status != 200) {
                log.error("Gemini respondió {} — body: {}", status,
                    response.body().substring(0, Math.min(200, response.body().length())));
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El asistente no está disponible en este momento. Inténtalo de nuevo.");
            }

            return parseResponse(response.body());

        } catch (RateLimitException | ResponseStatusException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            log.warn("Timeout tras {}s esperando respuesta de Gemini", timeoutSegundos);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                "El asistente tardó demasiado en responder. Inténtalo de nuevo.");
        } catch (Exception e) {
            log.error("Error inesperado llamando a Gemini: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El asistente no está disponible en este momento. Inténtalo de nuevo.");
        }
    }

    private ObjectNode buildRequestBody(List<Map<String, Object>> messages,
                                        List<Map<String, Object>> tools) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();

        // Gemini separa el system prompt del historial de mensajes
        messages.stream()
            .filter(m -> "system".equals(m.get("role")))
            .map(m -> (String) m.get("content"))
            .findFirst()
            .ifPresent(text -> {
                ObjectNode si = objectMapper.createObjectNode();
                ArrayNode parts = objectMapper.createArrayNode();
                ObjectNode part = objectMapper.createObjectNode();
                part.put("text", text);
                parts.add(part);
                si.set("parts", parts);
                body.set("systemInstruction", si);
            });

        body.set("contents", convertMessages(messages));

        if (!tools.isEmpty()) {
            body.set("tools", convertTools(tools));
            ObjectNode toolConfig = objectMapper.createObjectNode();
            ObjectNode fcc = objectMapper.createObjectNode();
            fcc.put("mode", "AUTO");
            toolConfig.set("functionCallingConfig", fcc);
            body.set("toolConfig", toolConfig);
        }

        ObjectNode genConfig = objectMapper.createObjectNode();
        genConfig.put("maxOutputTokens", maxTokens);
        genConfig.put("temperature", 0.7);
        body.set("generationConfig", genConfig);

        return body;
    }

    private ArrayNode convertMessages(List<Map<String, Object>> messages) throws Exception {
        ArrayNode contents = objectMapper.createArrayNode();
        int i = 0;

        while (i < messages.size()) {
            Map<String, Object> msg = messages.get(i);
            String role = (String) msg.get("role");

            if ("system".equals(role)) {
                i++;
                continue;
            }

            if ("tool".equals(role)) {
                // Gemini exige un único mensaje user con todos los functionResponse consecutivos
                ObjectNode userMsg = objectMapper.createObjectNode();
                userMsg.put("role", "user");
                ArrayNode parts = objectMapper.createArrayNode();

                while (i < messages.size() && "tool".equals(messages.get(i).get("role"))) {
                    Map<String, Object> toolMsg = messages.get(i);
                    ObjectNode funcRespPart = objectMapper.createObjectNode();
                    ObjectNode funcResp = objectMapper.createObjectNode();
                    funcResp.put("name", (String) toolMsg.get("name"));
                    ObjectNode responseContent = objectMapper.createObjectNode();
                    responseContent.put("output", (String) toolMsg.get("content"));
                    funcResp.set("response", responseContent);
                    funcRespPart.set("functionResponse", funcResp);
                    parts.add(funcRespPart);
                    i++;
                }

                userMsg.set("parts", parts);
                contents.add(userMsg);
                continue;
            }

            contents.add(convertMensaje(msg));
            i++;
        }

        return contents;
    }

    private ObjectNode convertMensaje(Map<String, Object> msg) throws Exception {
        String role = (String) msg.get("role");
        ObjectNode contentNode = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();

        if ("assistant".equals(role)) {
            contentNode.put("role", "model");
            Object toolCallsObj = msg.get("tool_calls");

            if (toolCallsObj instanceof JsonNode jn && jn.isArray()) {
                for (JsonNode call : jn) {
                    ObjectNode funcCallPart = objectMapper.createObjectNode();
                    ObjectNode funcCall = objectMapper.createObjectNode();
                    funcCall.put("name", call.path("function").path("name").asText());
                    String argsStr = call.path("function").path("arguments").asText("{}");
                    funcCall.set("args", objectMapper.readTree(argsStr));
                    funcCallPart.set("functionCall", funcCall);
                    parts.add(funcCallPart);
                }
            } else {
                Object content = msg.get("content");
                String text = content != null ? content.toString() : "";
                ObjectNode textPart = objectMapper.createObjectNode();
                // Gemini rechaza parts con texto vacío
                textPart.put("text", text.isBlank() ? " " : text);
                parts.add(textPart);
            }
        } else {
            contentNode.put("role", "user");
            String text = (String) msg.get("content");
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("text", text != null ? text : "");
            parts.add(textPart);
        }

        contentNode.set("parts", parts);
        return contentNode;
    }

    @SuppressWarnings("unchecked")
    private ArrayNode convertTools(List<Map<String, Object>> tools) {
        ArrayNode toolsArray = objectMapper.createArrayNode();
        ObjectNode toolDef = objectMapper.createObjectNode();
        ArrayNode funcDecls = objectMapper.createArrayNode();

        for (Map<String, Object> tool : tools) {
            Map<String, Object> func = (Map<String, Object>) tool.get("function");
            if (func == null) continue;

            ObjectNode decl = objectMapper.createObjectNode();
            decl.put("name", (String) func.get("name"));
            decl.put("description", (String) func.get("description"));
            decl.set("parameters", objectMapper.valueToTree(func.get("parameters")));
            funcDecls.add(decl);
        }

        toolDef.set("functionDeclarations", funcDecls);
        toolsArray.add(toolDef);
        return toolsArray;
    }

    private JsonNode parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode candidates = root.path("candidates");

        if (candidates.isMissingNode() || candidates.isEmpty()) {
            log.warn("Gemini respuesta sin candidates — body: {}",
                body.substring(0, Math.min(200, body.length())));
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El asistente no pudo generar una respuesta. Inténtalo de nuevo.");
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (parts.isMissingNode() || parts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El asistente no pudo generar una respuesta. Inténtalo de nuevo.");
        }

        // Devuelve nodo en formato OpenAI-compatible para que AsistenteIAService no dependa del proveedor
        ObjectNode messageNode = objectMapper.createObjectNode();

        boolean hasFunctionCalls = false;
        for (JsonNode part : parts) {
            if (part.has("functionCall")) { hasFunctionCalls = true; break; }
        }

        if (hasFunctionCalls) {
            ArrayNode toolCalls = objectMapper.createArrayNode();
            int idx = 0;
            for (JsonNode part : parts) {
                if (!part.has("functionCall")) continue;
                JsonNode fc = part.path("functionCall");
                ObjectNode call = objectMapper.createObjectNode();
                call.put("id", "call_" + idx++);
                call.put("type", "function");
                ObjectNode func = objectMapper.createObjectNode();
                func.put("name", fc.path("name").asText());
                func.put("arguments", objectMapper.writeValueAsString(fc.path("args")));
                call.set("function", func);
                toolCalls.add(call);
            }
            messageNode.set("tool_calls", toolCalls);
            log.info("Gemini devolvió {} llamada(s) a herramientas", toolCalls.size());
        } else {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : parts) {
                if (part.has("text")) sb.append(part.path("text").asText());
            }
            messageNode.put("content", sb.toString());
            log.info("Gemini respondió con texto ({} chars)", sb.length());
        }

        return messageNode;
    }

    /**
     * Calcula los segundos de cooldown para un 429 de Gemini.
     *
     * Prioridad:
     * 1. Cuota diaria agotada (quotaId contiene "PerDay") → cooldown hasta medianoche Pacific,
     *    que es cuando Gemini Free Tier resetea su cuota diaria. El retryDelay del RPM no sirve
     *    aquí porque en 34s Gemini volvería a fallar por el límite diario.
     * 2. retryDelay en RetryInfo → límite de RPM o tokens por minuto, recuperable en segundos.
     * 3. 60s como último recurso si el body no es parseable.
     */
    int calcularRetryAfter(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode details = root.path("error").path("details");

            // 1. Cuota diaria: buscar violation con quotaId que contenga "PerDay"
            if (details.isArray()) {
                for (JsonNode detail : details) {
                    if ("type.googleapis.com/google.rpc.QuotaFailure".equals(detail.path("@type").asText())) {
                        for (JsonNode v : detail.path("violations")) {
                            if (v.path("quotaId").asText("").contains("PerDay")) {
                                ZonedDateTime ahoraPacific = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
                                ZonedDateTime resetPacific = ahoraPacific.toLocalDate().plusDays(1)
                                    .atStartOfDay(ZoneId.of("America/Los_Angeles"));
                                int segundos = (int) Duration.between(ahoraPacific, resetPacific).getSeconds();
                                log.warn("Gemini cuota diaria agotada — cooldown hasta reset (medianoche Pacific): {}s (~{}h). "
                                    + "Para recuperarla antes: nuevo proyecto en aistudio.google.com/app/apikey",
                                    segundos, segundos / 3600);
                                return segundos;
                            }
                        }
                    }
                }
            }

            // 2. Rate limit de RPM/TPM: usar retryDelay de RetryInfo
            if (details.isArray()) {
                for (JsonNode detail : details) {
                    if ("type.googleapis.com/google.rpc.RetryInfo".equals(detail.path("@type").asText())) {
                        String digits = detail.path("retryDelay").asText("").replaceAll("[^0-9]", "");
                        if (!digits.isBlank()) {
                            int segundos = Integer.parseInt(digits);
                            log.warn("Gemini rate limit (RPM/TPM) — reintentando en {}s", segundos);
                            return segundos;
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.debug("No se pudo parsear el body del 429 de Gemini: {}", e.getMessage());
        }

        // 3. Último recurso
        log.warn("Gemini HTTP 429 — sin información de reset, cooldown de 60s");
        return 60;
    }

    static final class RateLimitException extends RuntimeException {
        private final int retryAfter;
        RateLimitException(int retryAfter) {
            super("Rate limit — Retry-After: " + retryAfter + "s");
            this.retryAfter = retryAfter;
        }
        int getRetryAfter() { return retryAfter; }
    }
}
