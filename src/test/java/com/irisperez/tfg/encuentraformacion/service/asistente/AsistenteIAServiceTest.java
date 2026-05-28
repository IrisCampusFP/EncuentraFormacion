package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irisperez.tfg.encuentraformacion.dto.asistente.RespuestaAsistenteDTO;
import com.irisperez.tfg.encuentraformacion.dto.asistente.SesionIAResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.SesionIA;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.repository.EstudianteRepository;
import com.irisperez.tfg.encuentraformacion.repository.MensajeIARepository;
import com.irisperez.tfg.encuentraformacion.repository.SesionIARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsistenteIAService")
class AsistenteIAServiceTest {

    @Mock private SesionIARepository sesionRepository;
    @Mock private MensajeIARepository mensajeRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private LlmClient llmClient;
    @Mock private AsistenteToolService toolService;
    @Mock private AsistenteContextoService contextoService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AsistenteIAService service;

    private Estudiante estudiante;
    private SesionIA sesion;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Test");

        estudiante = new Estudiante();
        estudiante.setId(10L);
        estudiante.setUsuario(usuario);

        sesion = new SesionIA(estudiante, "Nueva conversación");
        var idField = SesionIA.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(sesion, 5L);
    }

    @Test
    @DisplayName("crearSesion devuelve DTO con título por defecto")
    void crearSesionDevuelveSesionConTituloDefecto() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.save(any(SesionIA.class))).thenReturn(sesion);
        when(mensajeRepository.countBySesionId(any())).thenReturn(0L);

        SesionIAResumenDTO resultado = service.crearSesion(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTitulo()).isEqualTo("Nueva conversación");
    }

    @Test
    @DisplayName("enviarMensaje sin tool_call guarda mensajes y devuelve respuesta")
    void enviarMensajeSinToolCallGuardaMensajesYDevuelveRespuesta() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.findByIdAndEstudianteId(5L, 10L)).thenReturn(Optional.of(sesion));
        when(mensajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mensajeRepository.findTop20BySesionIdOrderByFechaEnvioAsc(any())).thenReturn(List.of());
        when(contextoService.buildSystemPrompt(1L)).thenReturn("Prompt del sistema");

        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("content", "Hola, soy el asistente.");
        when(llmClient.llamar(any(), any())).thenReturn(messageNode);

        RespuestaAsistenteDTO resp = service.enviarMensaje(5L, "Hola", 1L);

        assertThat(resp).isNotNull();
        assertThat(resp.getContenido()).isEqualTo("Hola, soy el asistente.");
    }

    @Test
    @DisplayName("enviarMensaje con tool_call ejecuta herramienta y hace segunda llamada")
    void enviarMensajeConToolCallEjecutaHerramientaYHaceSegundaLlamada() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.findByIdAndEstudianteId(5L, 10L)).thenReturn(Optional.of(sesion));
        when(mensajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mensajeRepository.findTop20BySesionIdOrderByFechaEnvioAsc(any())).thenReturn(List.of());
        when(contextoService.buildSystemPrompt(1L)).thenReturn("Prompt");

        ObjectNode toolCall = mapper.createObjectNode();
        toolCall.put("id", "call_123");
        toolCall.put("type", "function");
        ObjectNode functionNode = mapper.createObjectNode();
        functionNode.put("name", "buscarFormaciones");
        functionNode.put("arguments", "{}");
        toolCall.set("function", functionNode);

        ObjectNode firstMessage = mapper.createObjectNode();
        firstMessage.set("tool_calls", mapper.createArrayNode().add(toolCall));

        ObjectNode secondMessage = mapper.createObjectNode();
        secondMessage.put("content", "Aquí tienes las formaciones encontradas.");

        when(llmClient.llamar(any(), any()))
            .thenReturn(firstMessage)
            .thenReturn(secondMessage);
        when(toolService.ejecutar(eq("buscarFormaciones"), any(JsonNode.class)))
            .thenReturn("Resultados: 1 formación.");

        RespuestaAsistenteDTO resp = service.enviarMensaje(5L, "Busca formaciones", 1L);

        assertThat(resp.getContenido()).contains("formaciones encontradas");
        verify(toolService).ejecutar(eq("buscarFormaciones"), any(JsonNode.class));
    }

    @Test
    @DisplayName("enviarMensaje conserva el título 'Conversación N · DD mes' generado al crear la sesión")
    void enviarMensajeConservaTituloGeneradoAlCrear() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.findByIdAndEstudianteId(5L, 10L)).thenReturn(Optional.of(sesion));
        when(mensajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mensajeRepository.findTop20BySesionIdOrderByFechaEnvioAsc(any())).thenReturn(List.of());
        when(contextoService.buildSystemPrompt(1L)).thenReturn("Prompt");

        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("content", "Respuesta.");
        when(llmClient.llamar(any(), any())).thenReturn(messageNode);

        service.enviarMensaje(5L, "¿Qué puedo estudiar con bachillerato?", 1L);

        assertThat(sesion.getTitulo()).isEqualTo("Nueva conversación");
    }

    @Test
    @DisplayName("getHistorial de sesión ajena lanza 403")
    void getHistorialSesionAjenaLanzaForbidden() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.findByIdAndEstudianteId(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHistorial(5L, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("eliminarSesion ajena lanza 403")
    void eliminarSesionAjenaLanzaForbidden() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.findByIdAndEstudianteId(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarSesion(5L, 1L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("enviarMensaje con tool_call que tiene precioMax null no pasa null al servicio de tools")
    void enviarMensajeConToolCallConPrecioMaxNullSanitizaArgs() throws Exception {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.findByIdAndEstudianteId(5L, 10L)).thenReturn(Optional.of(sesion));
        when(mensajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mensajeRepository.findTop20BySesionIdOrderByFechaEnvioAsc(any())).thenReturn(List.of());
        when(contextoService.buildSystemPrompt(1L)).thenReturn("Prompt");

        // Simula Gemini enviando precioMax: null en los args (comportamiento real observado)
        ObjectNode toolCall = mapper.createObjectNode();
        toolCall.put("id", "call_001");
        toolCall.put("type", "function");
        ObjectNode functionNode = mapper.createObjectNode();
        functionNode.put("name", "buscarFormaciones");
        // Args con precioMax explícitamente null — lo que envía Gemini cuando no hay precio
        functionNode.put("arguments", "{\"nombre\":\"programacion\",\"precioMax\":null}");
        toolCall.set("function", functionNode);

        ObjectNode firstMessage = mapper.createObjectNode();
        firstMessage.set("tool_calls", mapper.createArrayNode().add(toolCall));

        ObjectNode secondMessage = mapper.createObjectNode();
        secondMessage.put("content", "Encontré formaciones de programación.");

        when(llmClient.llamar(any(), any()))
            .thenReturn(firstMessage)
            .thenReturn(secondMessage);
        when(toolService.ejecutar(eq("buscarFormaciones"), any()))
            .thenReturn("3 formaciones encontradas.");

        service.enviarMensaje(5L, "Busca formaciones de programación", 1L);

        // Verifica que los args que llegan a toolService NO tienen precioMax (fue eliminado por ser null)
        var argsCaptor = org.mockito.ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
        verify(toolService).ejecutar(eq("buscarFormaciones"), argsCaptor.capture());
        assertThat(argsCaptor.getValue().has("precioMax")).isFalse();
        assertThat(argsCaptor.getValue().path("nombre").asText()).isEqualTo("programacion");
    }

    @Test
    @DisplayName("enviarMensaje sin tool_calls responde directamente (preguntas exploratorias con tool_choice auto)")
    void enviarMensajeSinToolCallsRespuestaDirecta() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.findByIdAndEstudianteId(5L, 10L)).thenReturn(Optional.of(sesion));
        when(mensajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mensajeRepository.findTop20BySesionIdOrderByFechaEnvioAsc(any())).thenReturn(List.of());
        when(contextoService.buildSystemPrompt(1L)).thenReturn("Prompt");

        ObjectNode textResponse = mapper.createObjectNode();
        textResponse.put("content", "Los sectores con más demanda son tecnología, salud y energía renovable.");
        when(llmClient.llamar(any(), any())).thenReturn(textResponse);

        RespuestaAsistenteDTO resp = service.enviarMensaje(5L, "¿Qué sectores tienen más demanda?", 1L);

        assertThat(resp.getContenido()).contains("tecnología");
        // El servicio de tools no se invoca cuando el modelo responde directamente
        verifyNoInteractions(toolService);
        // Solo una llamada al LLM (sin bucle de tool calling)
        verify(llmClient, times(1)).llamar(any(), any());
    }

    @Test
    @DisplayName("bucle multi-turno: 0 resultados en ronda 1 → reintenta en ronda 2 → responde")
    void bucleToolCallingReintentaConFiltrosMasAmplos() {
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(sesionRepository.findByIdAndEstudianteId(5L, 10L)).thenReturn(Optional.of(sesion));
        when(mensajeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mensajeRepository.findTop20BySesionIdOrderByFechaEnvioAsc(any())).thenReturn(List.of());
        when(contextoService.buildSystemPrompt(1L)).thenReturn("Prompt");

        ObjectNode toolCall1 = buildToolCall("call_r1", "buscarFormaciones",
            "{\"tipoEstudiosNombre\":\"FP Grado Superior\",\"provincia\":\"Madrid\"}");
        ObjectNode toolCall2 = buildToolCall("call_r2", "buscarFormaciones",
            "{\"tipoEstudiosNombre\":\"FP Grado Superior\"}");

        ObjectNode ronda1 = mapper.createObjectNode();
        ronda1.set("tool_calls", mapper.createArrayNode().add(toolCall1));

        ObjectNode ronda2 = mapper.createObjectNode();
        ronda2.set("tool_calls", mapper.createArrayNode().add(toolCall2));

        ObjectNode respuestaFinal = mapper.createObjectNode();
        respuestaFinal.put("content", "Encontré formaciones de FP Grado Superior en toda España.");

        when(llmClient.llamar(any(), any()))
            .thenReturn(ronda1)    // 1ª llamada LLM → busca con filtros estrechos
            .thenReturn(ronda2)    // 2ª llamada LLM → reintenta con filtros amplios
            .thenReturn(respuestaFinal); // 3ª llamada LLM → responde en texto

        when(toolService.ejecutar(eq("buscarFormaciones"), any()))
            .thenReturn("No se encontraron formaciones con esos criterios.")  // ronda 1: sin resultados
            .thenReturn("Resultados: 5 formaciones encontradas.");             // ronda 2: hay resultados

        RespuestaAsistenteDTO resp = service.enviarMensaje(5L, "Busca FP en Madrid", 1L);

        assertThat(resp.getContenido()).contains("España");
        verify(toolService, times(2)).ejecutar(eq("buscarFormaciones"), any());
        verify(llmClient, times(3)).llamar(any(), any());
    }

    private ObjectNode buildToolCall(String id, String funcName, String arguments) {
        ObjectNode call = mapper.createObjectNode();
        call.put("id", id);
        call.put("type", "function");
        ObjectNode func = mapper.createObjectNode();
        func.put("name", funcName);
        func.put("arguments", arguments);
        call.set("function", func);
        return call;
    }
}
