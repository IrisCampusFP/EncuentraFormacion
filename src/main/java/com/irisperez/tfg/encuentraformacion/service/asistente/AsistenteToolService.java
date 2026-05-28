package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionDetalleDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionFiltroDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.entity.TipoEstudios;
import com.irisperez.tfg.encuentraformacion.repository.ComunidadAutonomaRepository;
import com.irisperez.tfg.encuentraformacion.repository.ProvinciaRepository;
import com.irisperez.tfg.encuentraformacion.repository.TipoEstudiosRepository;
import com.irisperez.tfg.encuentraformacion.security.LogSanitizer;
import com.irisperez.tfg.encuentraformacion.service.formacion.FormacionPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsistenteToolService {

    private final FormacionPublicService formacionPublicService;
    private final TipoEstudiosRepository tipoEstudiosRepository;
    private final ProvinciaRepository provinciaRepository;
    private final ComunidadAutonomaRepository comunidadAutonomaRepository;

    // Nombres coloquiales que usan los LLM → nombre exacto almacenado en BD (norma INE)
    private static final Map<String, String> CCAA_ALIAS = Map.ofEntries(
        Map.entry("comunidad de madrid",           "Madrid, Comunidad de"),
        Map.entry("madrid",                        "Madrid, Comunidad de"),
        Map.entry("asturias",                      "Asturias, Principado de"),
        Map.entry("principado de asturias",        "Asturias, Principado de"),
        Map.entry("baleares",                      "Balears, Illes"),
        Map.entry("islas baleares",                "Balears, Illes"),
        Map.entry("illes balears",                 "Balears, Illes"),
        Map.entry("murcia",                        "Murcia, Región de"),
        Map.entry("región de murcia",              "Murcia, Región de"),
        Map.entry("navarra",                       "Navarra, Comunidad Foral de"),
        Map.entry("comunidad foral de navarra",    "Navarra, Comunidad Foral de"),
        Map.entry("la rioja",                      "Rioja, La"),
        Map.entry("comunidad valenciana",          "Comunitat Valenciana"),
        Map.entry("valenciana",                    "Comunitat Valenciana"),
        Map.entry("valencia",                      "Comunitat Valenciana"),
        Map.entry("pais vasco",                    "País Vasco"),
        Map.entry("euskadi",                       "País Vasco")
    );

    /**
     * Definiciones de herramientas en formato OpenAI (GeminiClient las convierte internamente).
     */
    public static final List<Map<String, Object>> TOOL_DEFINITIONS = List.of(
        Map.of(
            "type", "function",
            "function", Map.of(
                "name", "buscarFormaciones",
                "description", "Busca formaciones disponibles en la plataforma. Úsala SIEMPRE antes de recomendar formaciones concretas.",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "tipoEstudiosNombre", Map.of("type", "string",
                            "description", "Tipo de estudios. Valores exactos: 'Educación Infantil 1er ciclo', 'Educación Infantil 2º ciclo', 'Educación Primaria', 'ESO', 'Bachillerato', 'FP Básica', 'FP Grado Medio', 'FP Grado Superior', 'Educación Especial', 'Idiomas', 'Música y Artes', 'Enseñanzas Deportivas', 'Educación de Adultos', 'Curso / Formación no reglada', 'Certificado de Profesionalidad', 'Grado Universitario', 'Máster Oficial', 'Máster / Título propio', 'Doctorado'. IMPORTANTE: los ciclos formativos de informática (DAW, DAM, ASIR) y todos los CFGS son 'FP Grado Superior', NO 'Grado Universitario'. 'Grado Universitario' es solo para grados impartidos por universidades (ADE, Medicina, Ingeniería, etc.)."),
                        "modalidad", Map.of("type", "string",
                            "description", "PRESENCIAL, SEMIPRESENCIAL o DISTANCIA"),
                        "comunidadAutonoma", Map.of("type", "string",
                            "description", "Nombre de la comunidad autónoma. Usa la forma natural (Madrid, Andalucía, Cataluña, Asturias, Baleares, Murcia, Navarra, La Rioja, Comunidad Valenciana, País Vasco, etc.). El sistema normaliza automáticamente."),
                        "provincia", Map.of("type", "string",
                            "description", "Nombre de la provincia española donde buscar (más específico que comunidad autónoma)"),
                        "precioMax", Map.of("type", "string",
                            "description", "Precio máximo en euros por año, como número (ej: '500'). Omitir si no hay restricción"),
                        "nombre", Map.of("type", "string",
                            "description", "Un único término para buscar en el nombre de la formación. Usa 'nombres' en su lugar si quieres cubrir sinónimos."),
                        "nombres", Map.of("type", "array",
                            "items", Map.of("type", "string"),
                            "description", "Lista de términos alternativos para buscar (OR). Úsalo cuando el área de interés tiene varios nombres equivalentes. Ejemplo: [\"informática\", \"sistemas informáticos\", \"redes\", \"desarrollo\", \"software\"]. Máximo 6 términos.")
                    ),
                    "required", List.of()
                )
            )
        ),
        Map.of(
            "type", "function",
            "function", Map.of(
                "name", "getDetalleFormacion",
                "description", "Obtiene descripción completa, duración, precio y datos del centro de una formación concreta.",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "formacionUuid", Map.of("type", "string",
                            "description", "UUID de la formación (obtenido de buscarFormaciones)")
                    ),
                    "required", List.of("formacionUuid")
                )
            )
        )
    );

    /**
     * Dispatcher: ejecuta la herramienta indicada con los argumentos dados.
     */
    public String ejecutar(String toolName, JsonNode argumentos) {
        return switch (toolName) {
            case "buscarFormaciones"   -> buscarFormaciones(argumentos);
            case "getDetalleFormacion" -> getDetalleFormacion(argumentos);
            default -> "Herramienta desconocida: " + LogSanitizer.sanitize(toolName);
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Herramientas privadas
    // ─────────────────────────────────────────────────────────────────────────

    private String buscarFormaciones(JsonNode args) {
        FormacionFiltroDTO filtro = new FormacionFiltroDTO();

        // Resolver tipo de estudios por nombre (null-safe)
        String tipoEstudiosNombre = args.path("tipoEstudiosNombre").asText(null);
        if (tipoEstudiosNombre != null && !tipoEstudiosNombre.isBlank()) {
            Optional<TipoEstudios> tipo = tipoEstudiosRepository.findByNombreIgnoreCase(tipoEstudiosNombre);
            tipo.ifPresent(t -> filtro.setTipoEstudiosId(t.getId()));
        }

        // Resolver comunidad autónoma por nombre (null-safe), normalizando alias coloquiales
        String ccaaNombre = args.path("comunidadAutonoma").asText(null);
        if (ccaaNombre != null && !ccaaNombre.isBlank()) {
            String ccaaResolved = CCAA_ALIAS.getOrDefault(ccaaNombre.toLowerCase().trim(), ccaaNombre);
            Optional<ComunidadAutonoma> ccaa = comunidadAutonomaRepository.findByNombreIgnoreCase(ccaaResolved);
            ccaa.ifPresent(c -> filtro.setComunidadAutonomaId(c.getId()));
        }

        // Resolver provincia por nombre (null-safe) — más específico, tiene precedencia sobre CCAA
        String provinciaNombre = args.path("provincia").asText(null);
        if (provinciaNombre != null && !provinciaNombre.isBlank()) {
            Optional<Provincia> provincia = provinciaRepository.findByNombreIgnoreCase(provinciaNombre);
            provincia.ifPresent(p -> filtro.setProvinciaId(p.getId()));
        }

        // Modalidad
        String modalidadStr = args.path("modalidad").asText(null);
        if (modalidadStr != null && !modalidadStr.isBlank()) {
            try {
                filtro.setModalidad(ModalidadFormacion.valueOf(modalidadStr.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Ignorar modalidad desconocida
            }
        }

        // Precio máximo — el LLM puede enviar string o number; asDouble() coerciona ambos
        JsonNode precioNode = args.path("precioMax");
        if (!precioNode.isMissingNode() && !precioNode.isNull()) {
            try {
                double precioMaxRaw = Double.parseDouble(precioNode.asText("-1"));
                if (precioMaxRaw >= 0) {
                    filtro.setPrecioMax(BigDecimal.valueOf(precioMaxRaw));
                }
            } catch (NumberFormatException ignored) { /* valor no numérico, ignorar */ }
        }

        // Multi-término (OR) — tiene precedencia sobre nombre único
        JsonNode nombresNode = args.path("nombres");
        if (nombresNode.isArray() && !nombresNode.isEmpty()) {
            List<String> terminos = new ArrayList<>();
            for (JsonNode n : nombresNode) {
                String t = n.asText(null);
                if (t != null && !t.isBlank()) terminos.add(t);
            }
            if (!terminos.isEmpty()) {
                filtro.setNombres(terminos);
            }
        }

        // Nombre único (fallback cuando no se usa 'nombres')
        if (filtro.getNombres() == null) {
            String nombreStr = args.path("nombre").asText(null);
            if (nombreStr != null && !nombreStr.isBlank()) {
                filtro.setNombre(nombreStr);
            }
        }

        Page<FormacionResumenDTO> resultados =
            formacionPublicService.buscar(filtro, PageRequest.of(0, 9), "recientes");

        if (resultados.isEmpty()) {
            return "No se encontraron formaciones con esos criterios. "
                + "Prueba a ampliar la búsqueda (sin provincia, sin precio máximo, etc.).";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Resultados (").append(resultados.getTotalElements())
          .append(" formaciones encontradas en la plataforma):\n");

        int pos = 1;
        for (FormacionResumenDTO f : resultados.getContent()) {
            String precio = (f.getPrecio() == null || f.getPrecio().compareTo(BigDecimal.ZERO) == 0)
                ? "gratuito"
                : f.getPrecio().toPlainString() + "€/año";
            String tipoNombre = f.getTipoEstudios() != null ? f.getTipoEstudios().getNombre() : "-";
            String modalidadNombre = f.getModalidad() != null ? f.getModalidad().name() : "-";

            sb.append(pos++).append(". [FORMACION:").append(f.getUuid()).append("] \"")
              .append(f.getNombre()).append("\" | ").append(tipoNombre)
              .append(" | ").append(modalidadNombre).append("\n")
              .append("   Centro: ").append(f.getCentroNombre())
              .append(", ").append(f.getCentroLocalidad() != null ? f.getCentroLocalidad() : "-")
              .append(" | Precio: ").append(precio).append("\n");
        }

        return sb.toString();
    }

    private String getDetalleFormacion(JsonNode args) {
        String uuidStr = args.path("formacionUuid").asText(null);
        if (uuidStr == null || uuidStr.isBlank()) {
            return "Error: el parámetro formacionUuid es obligatorio.";
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return "Error: el UUID proporcionado no tiene el formato correcto.";
        }

        try {
            FormacionDetalleDTO f = formacionPublicService.findByUuid(uuid);
            String precio = (f.getPrecio() == null || f.getPrecio().compareTo(BigDecimal.ZERO) == 0)
                ? "gratuito"
                : f.getPrecio().toPlainString() + "€/año";
            String tipoNombre  = f.getTipoEstudios() != null ? f.getTipoEstudios().getNombre() : "-";
            String modalidad   = f.getModalidad() != null ? f.getModalidad().name() : "-";
            String horario     = f.getHorario() != null ? f.getHorario().name() : "-";
            String duracion    = f.getDuracionHoras() != null ? f.getDuracionHoras() + "h" : "-";

            return "[FORMACION:" + f.getUuid() + "] \"" + f.getNombre() + "\"\n"
                + "Tipo: " + tipoNombre + " | Modalidad: " + modalidad + " | Horario: " + horario + "\n"
                + "Centro: " + f.getCentroNombre() + ", " + f.getCentroLocalidad()
                    + ", " + f.getCentroProvincia() + "\n"
                + "Duración: " + duracion + " | Precio: " + precio + "\n"
                + "Descripción: " + (f.getDescripcion() != null ? f.getDescripcion() : "Sin descripción");
        } catch (Exception e) {
            return "No se encontró la formación solicitada o ocurrió un error al obtener los detalles.";
        }
    }
}
