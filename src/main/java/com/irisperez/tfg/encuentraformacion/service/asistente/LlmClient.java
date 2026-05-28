package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public interface LlmClient {
    JsonNode llamar(List<Map<String, Object>> messages, List<Map<String, Object>> tools);
}
