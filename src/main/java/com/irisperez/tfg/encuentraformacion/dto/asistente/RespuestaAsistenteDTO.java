package com.irisperez.tfg.encuentraformacion.dto.asistente;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RespuestaAsistenteDTO {
    private String contenido;             // texto completo con tokens [FORMACION:uuid]
    private List<String> formacionUuids;  // UUIDs extraídos para que el frontend renderice cards
}
