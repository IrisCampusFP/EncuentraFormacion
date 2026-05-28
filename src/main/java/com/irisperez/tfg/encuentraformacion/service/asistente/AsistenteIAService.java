package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.irisperez.tfg.encuentraformacion.dto.asistente.HistorialSesionDTO;
import com.irisperez.tfg.encuentraformacion.exception.AsistenteRateLimitException;
import com.irisperez.tfg.encuentraformacion.dto.asistente.MensajeIADTO;
import com.irisperez.tfg.encuentraformacion.dto.asistente.RespuestaAsistenteDTO;
import com.irisperez.tfg.encuentraformacion.dto.asistente.SesionIAResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.MensajeIA;
import com.irisperez.tfg.encuentraformacion.model.entity.SesionIA;
import com.irisperez.tfg.encuentraformacion.model.enums.RolMensajeIA;
import com.irisperez.tfg.encuentraformacion.repository.EstudianteRepository;
import com.irisperez.tfg.encuentraformacion.repository.MensajeIARepository;
import com.irisperez.tfg.encuentraformacion.repository.SesionIARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsistenteIAService {

    private final SesionIARepository sesionRepository;
    private final MensajeIARepository mensajeRepository;
    private final EstudianteRepository estudianteRepository;
    private final LlmClient llmClient;
    private final AsistenteToolService toolService;
    private final AsistenteContextoService contextoService;
    private final ObjectMapper objectMapper;

    private static final Pattern FORMACION_PATTERN = Pattern.compile("\\[FORMACION:([a-fA-F0-9\\-]{36})\\]");

    @Transactional
    public SesionIAResumenDTO crearSesion(Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        long numSesiones = sesionRepository.countByEstudianteId(est.getId());
        String titulo = generarTituloInicial(numSesiones + 1);
        SesionIA sesion = new SesionIA(est, titulo);
        sesion = sesionRepository.save(sesion);

        return mapToResumenDTO(sesion);
    }

    @Transactional
    public SesionIAResumenDTO renombrarSesion(Long sesionId, String nuevoTitulo, Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        SesionIA sesion = sesionRepository.findByIdAndEstudianteId(sesionId, est.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta sesión"));

        sesion.setTitulo(nuevoTitulo.trim());
        sesionRepository.save(sesion);

        return mapToResumenDTO(sesion);
    }

    @Transactional(readOnly = true)
    public List<SesionIAResumenDTO> listarSesiones(Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        return sesionRepository.findByEstudianteIdOrderByUltimaActividadDesc(est.getId())
            .stream()
            .map(this::mapToResumenDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public RespuestaAsistenteDTO enviarMensaje(Long sesionId, String contenido, Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        SesionIA sesion = sesionRepository.findByIdAndEstudianteId(sesionId, est.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta sesión"));

        // 1. Guardar mensaje del usuario
        MensajeIA msgUsuario = new MensajeIA(sesion, RolMensajeIA.USER, contenido);
        mensajeRepository.save(msgUsuario);

        // Actualizar última actividad
        sesion.setUltimaActividad(java.time.LocalDateTime.now());
        sesionRepository.save(sesion);

        // 2. Preparar contexto para el LLM
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", contextoService.buildSystemPrompt(usuarioId)));

        // Historial (últimos 20 mensajes)
        List<MensajeIA> historial = mensajeRepository.findTop20BySesionIdOrderByFechaEnvioAsc(sesionId);
        for (MensajeIA m : historial) {
            messages.add(Map.of("role", m.getRol().name().toLowerCase(), "content", m.getContenido()));
        }

        // 4. Bucle de tool calling: el modelo puede buscar, ver 0 resultados y reintentar
        //    con filtros más amplios antes de responder al usuario (máx. 3 rondas).
        JsonNode assistantResponse;
        try {
            assistantResponse = llmClient.llamar(messages, AsistenteToolService.TOOL_DEFINITIONS);
        } catch (GroqClient.ToolCallFailedException e) {
            log.warn("tool_use_failed en llamada inicial — respondiendo sin herramientas");
            try {
                assistantResponse = llmClient.llamar(messages, List.of());
            } catch (AsistenteRateLimitException re) {
                log.warn("Rate limit en reintento sin tools (llamada inicial)");
                assistantResponse = buildFallbackResponse(List.of());
            }
        } catch (AsistenteRateLimitException e) {
            log.warn("Rate limit en llamada inicial — devolviendo fallback");
            assistantResponse = buildFallbackResponse(List.of());
        }

        int rondas = 0;
        final int MAX_RONDAS = 3;
        List<String> uuidsDeHerramientas = new ArrayList<>();

        while (tieneToolCalls(assistantResponse) && rondas < MAX_RONDAS) {
            rondas++;
            ArrayNode toolCalls = (ArrayNode) assistantResponse.get("tool_calls");
            // Eliminar campos null para que Groq no rechace la tool call al recibirla en el historial
            ArrayNode toolCallsSanitized = sanitizeToolCalls(toolCalls);

            Map<String, Object> assistantMsgWithTools = new HashMap<>();
            assistantMsgWithTools.put("role", "assistant");
            assistantMsgWithTools.put("content", null);
            assistantMsgWithTools.put("tool_calls", toolCallsSanitized);
            messages.add(assistantMsgWithTools);

            for (JsonNode call : toolCallsSanitized) {
                String callId = call.get("id").asText();
                String funcName = call.get("function").get("name").asText();
                JsonNode args;
                try {
                    JsonNode parsed = objectMapper.readTree(call.get("function").get("arguments").asText());
                    args = stripNulls(parsed);
                } catch (Exception e) {
                    args = objectMapper.createObjectNode();
                }

                log.info("Ejecutando herramienta IA (ronda {}): {} con args: {}", rondas, funcName, args);
                String result = toolService.ejecutar(funcName, args);

                // Capturar UUIDs de los resultados de herramientas para usarlos aunque el LLM no los cite
                extraerUuids(result).stream()
                    .map(UUID::toString)
                    .filter(u -> !uuidsDeHerramientas.contains(u))
                    .forEach(uuidsDeHerramientas::add);

                messages.add(Map.of(
                    "role", "tool",
                    "tool_call_id", callId,
                    "name", funcName,
                    "content", result
                ));
            }

            // En la última ronda forzamos respuesta en texto; si no, el modelo puede seguir buscando
            List<Map<String, Object>> nextTools = (rondas < MAX_RONDAS)
                ? AsistenteToolService.TOOL_DEFINITIONS
                : List.of();
            try {
                assistantResponse = llmClient.llamar(messages, nextTools);
            } catch (GroqClient.ToolCallFailedException e) {
                // El modelo generó tool call malformada — reintentar sin tools para que responda en texto
                log.warn("tool_use_failed en ronda {} — reintentando sin herramientas", rondas);
                try {
                    assistantResponse = llmClient.llamar(messages, List.of());
                } catch (AsistenteRateLimitException re) {
                    log.warn("Rate limit en reintento sin tools tras ronda {}", rondas);
                    assistantResponse = buildFallbackResponse(uuidsDeHerramientas);
                }
                break;
            } catch (AsistenteRateLimitException e) {
                log.warn("Rate limit en ronda {} — saliendo con resultados ya recogidos", rondas);
                assistantResponse = buildFallbackResponse(uuidsDeHerramientas);
                break;
            }
        }

        // Si el LLM terminó el bucle con tool_calls pendientes (sin respuesta en texto), usar fallback
        if (tieneToolCalls(assistantResponse)) {
            assistantResponse = buildFallbackResponse(uuidsDeHerramientas);
        }

        JsonNode contentNode = assistantResponse.path("content");
        String respuestaFinal = (contentNode.isNull() || contentNode.isMissingNode())
                ? "Lo siento, no he podido procesar tu solicitud."
                : contentNode.asText().strip();
        if (respuestaFinal.isBlank() || parecerArtefactoToolCall(respuestaFinal)) {
            respuestaFinal = buildFallbackResponse(uuidsDeHerramientas).path("content").asText();
        }

        // 6. Extraer formaciones citadas; si el LLM no incluyó los tokens, usar los de la herramienta
        List<String> uuidsCitadosEnRespuesta = extraerUuids(respuestaFinal).stream()
            .map(UUID::toString)
            .collect(Collectors.toList());
        List<String> formacionesCitadas = uuidsCitadosEnRespuesta.isEmpty()
            ? uuidsDeHerramientas
            : uuidsCitadosEnRespuesta;

        // Garantizar que los UUIDs que se muestran como cards quedan siempre persistidos en el texto
        String contenidoAGuardar = respuestaFinal;
        if (!formacionesCitadas.isEmpty()) {
            Set<String> yaEnTexto = new HashSet<>(uuidsCitadosEnRespuesta);
            String tokensFaltantes = formacionesCitadas.stream()
                .filter(u -> !yaEnTexto.contains(u))
                .map(u -> "[FORMACION:" + u + "]")
                .collect(Collectors.joining(" "));
            if (!tokensFaltantes.isBlank()) {
                contenidoAGuardar = respuestaFinal + "\n" + tokensFaltantes;
            }
        }

        // 5. Guardar respuesta del asistente
        MensajeIA msgAsistente = new MensajeIA(sesion, RolMensajeIA.ASSISTANT, contenidoAGuardar);
        mensajeRepository.save(msgAsistente);

        return new RespuestaAsistenteDTO(respuestaFinal, formacionesCitadas);
    }

    @Transactional(readOnly = true)
    public HistorialSesionDTO getHistorial(Long sesionId, Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        SesionIA sesion = sesionRepository.findByIdAndEstudianteId(sesionId, est.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta sesión"));

        List<MensajeIADTO> mensajes = mensajeRepository.findBySesionIdOrderByFechaEnvioAsc(sesionId)
            .stream()
            .map(m -> new MensajeIADTO(m.getId(), m.getRol().name(), m.getContenido(), m.getFechaEnvio()))
            .collect(Collectors.toList());

        return new HistorialSesionDTO(sesionId, sesion.getTitulo(), mensajes);
    }

    @Transactional
    public void eliminarTodasLasSesiones(Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        sesionRepository.deleteAllByEstudianteId(est.getId());
    }

    @Transactional
    public void eliminarSesion(Long sesionId, Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        SesionIA sesion = sesionRepository.findByIdAndEstudianteId(sesionId, est.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta sesión"));

        sesionRepository.delete(sesion);
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private static final String[] MESES = {"ene","feb","mar","abr","may","jun","jul","ago","sep","oct","nov","dic"};

    private String generarTituloInicial(long numero) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        String mes = MESES[hoy.getMonthValue() - 1];
        return "Conversación " + numero + " · " + hoy.getDayOfMonth() + " " + mes;
    }

    private SesionIAResumenDTO mapToResumenDTO(SesionIA s) {
        long numMensajes = mensajeRepository.countBySesionId(s.getId());
        return new SesionIAResumenDTO(s.getId(), s.getTitulo(), s.getFechaInicio(), s.getUltimaActividad(), numMensajes);
    }

    private List<UUID> extraerUuids(String texto) {
        List<UUID> uuids = new ArrayList<>();
        Matcher matcher = FORMACION_PATTERN.matcher(texto);
        while (matcher.find()) {
            try {
                uuids.add(UUID.fromString(matcher.group(1)));
            } catch (Exception ignored) {}
        }
        return uuids.stream().distinct().collect(Collectors.toList());
    }

    private static boolean tieneToolCalls(JsonNode node) {
        JsonNode tc = node.get("tool_calls");
        return tc != null && tc.isArray() && !tc.isEmpty();
    }

    // Detecta si el content es un artefacto de tool call que el LLM filtró en texto
    // (ej: "buscarFormaciones{"nombre":"informatica"}" o "<tool_call>...")
    private static final Pattern TOOL_ARTIFACT_PATTERN =
        Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*\\s*\\{|<tool_call|<function_calls", Pattern.CASE_INSENSITIVE);

    private static boolean parecerArtefactoToolCall(String texto) {
        return TOOL_ARTIFACT_PATTERN.matcher(texto.stripLeading()).find();
    }

    /** Elimina campos con valor null de un ObjectNode (nivel raíz). */
    private JsonNode stripNulls(JsonNode node) {
        if (!node.isObject()) return node;
        com.fasterxml.jackson.databind.node.ObjectNode copy = objectMapper.createObjectNode();
        node.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isNull()) {
                copy.set(entry.getKey(), entry.getValue());
            }
        });
        return copy;
    }

    private JsonNode buildFallbackResponse(List<String> uuidsRecogidos) {
        String texto = uuidsRecogidos.isEmpty()
            ? "Ahora mismo tengo mucha demanda y no he podido responder. Por favor, inténtalo de nuevo en unos minutos."
            : "He encontrado varias formaciones que podrían interesarte. Puedes ver los detalles a continuación.";
        return objectMapper.createObjectNode().put("content", texto);
    }

    /** Reconstruye el ArrayNode de tool_calls con los arguments sanitizados (sin nulls). */
    private ArrayNode sanitizeToolCalls(ArrayNode toolCalls) {
        ArrayNode sanitized = objectMapper.createArrayNode();
        for (JsonNode call : toolCalls) {
            com.fasterxml.jackson.databind.node.ObjectNode callCopy = call.deepCopy();
            try {
                JsonNode argsNode = objectMapper.readTree(
                    call.path("function").path("arguments").asText("{}"));
                JsonNode argsClean = stripNulls(argsNode);
                ((com.fasterxml.jackson.databind.node.ObjectNode) callCopy.get("function"))
                    .put("arguments", objectMapper.writeValueAsString(argsClean));
            } catch (Exception ignored) {}
            sanitized.add(callCopy);
        }
        return sanitized;
    }
}
