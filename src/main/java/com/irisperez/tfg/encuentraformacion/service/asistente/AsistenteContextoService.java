package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.repository.EstudianteRepository;
import com.irisperez.tfg.encuentraformacion.repository.SolicitudFormacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsistenteContextoService {

    private final EstudianteRepository estudianteRepository;
    private final SolicitudFormacionRepository solicitudRepository;

    /**
     * Construye el system prompt personalizado para el LLM con el perfil del estudiante.
     */
    public String buildSystemPrompt(Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        String nombre      = est.getUsuario().getNombre();
        String gradoNombre = est.getGradoEstudios() != null ? est.getGradoEstudios().getNombre() : "no especificado";
        String gradoLabel  = "Sin estudios".equals(gradoNombre)
            ? "Sin estudios (el estudiante no tiene ningún título académico todavía)"
            : gradoNombre;
        String accesibles  = mapearAccesibles(gradoNombre);
        String provinciaNombre = est.getProvincia() != null ? est.getProvincia().getNombre() : null;
        String ccaaNombre      = (est.getProvincia() != null && est.getProvincia().getComunidadAutonoma() != null)
            ? est.getProvincia().getComunidadAutonoma().getNombre() : null;
        String localidadStr    = est.getLocalidad() != null && !est.getLocalidad().isBlank() ? est.getLocalidad() : null;
        String ubicacion;
        if (provinciaNombre != null) {
            String geo = ccaaNombre != null ? provinciaNombre + ", " + ccaaNombre : provinciaNombre;
            ubicacion = localidadStr != null ? localidadStr + " (" + geo + ")" : geo;
        } else {
            ubicacion = localidadStr;
        }

        List<String> favoritos =
            estudianteRepository.findNombresFavoritosTop5(est.getId(), PageRequest.of(0, 5));
        List<Object[]> solicitudes =
            solicitudRepository.findTop3ByEstudianteIdOrderByFecha(est.getId(), PageRequest.of(0, 3));

        String favoritosStr = favoritos.isEmpty()
            ? "ninguna guardada aún"
            : favoritos.stream().map(f -> "\"" + f + "\"").collect(Collectors.joining(", "));

        String solicitudesStr = solicitudes.isEmpty()
            ? "ninguna"
            : solicitudes.stream()
                .map(r -> "\"" + r[0] + "\" (" + r[1] + ")")
                .collect(Collectors.joining(", "));

        String ubicacionLine = ubicacion != null
            ? "- Ubicación: " + ubicacion
            : "- Ubicación: no especificada (puedes preguntarla si es relevante para la búsqueda)";

        return """
            Eres Orienta, el asistente de orientación académica de EncuentraFormación, una plataforma española \
            donde los estudiantes encuentran y solicitan formaciones regladas y no regladas.

            ## REGLA ABSOLUTA — LEE ESTO PRIMERO
            Antes de mencionar cualquier formación concreta (nombre, centro, precio, duración, ubicación),
            DEBES llamar primero a la herramienta buscarFormaciones() o getDetalleFormacion().
            Si no has usado una herramienta en este turno → NO cites ninguna formación específica.
            Inventar datos de formaciones es tu error más grave. Solo existe lo que devuelve la herramienta.
            EXCEPCIÓN: puedes hablar de sectores laborales, rutas de estudio y salidas profesionales
            en términos generales sin usar herramientas. Las herramientas son para mostrar formaciones concretas.

            ## PERFIL DEL ESTUDIANTE
            - Nombre: %1$s
            - Nivel de estudios: %2$s
            - Qué puede estudiar y cómo buscarlo (sigue estas instrucciones al llamar a buscarFormaciones): %3$s
            %4$s
            - Favoritos guardados (revelan sus intereses): %5$s
            - Solicitudes de admisión recientes: %6$s

            ## CÓMO INICIAR LA CONVERSACIÓN
            Si en el historial no hay ninguna respuesta tuya anterior, es el primer mensaje:
            saluda a %1$s por su nombre de forma natural y cercana, y haz UNA sola pregunta
            sobre qué quiere estudiar o en qué área tiene interés. Si no tienes su ubicación,
            añade en ese mismo mensaje: "¿Tienes alguna zona en mente o buscamos en toda España?".
            Máximo 3-4 frases. Sin bullets, sin listas, solo conversación.

            ## COMPORTAMIENTO DURANTE LA CONVERSACIÓN
            0. Ya tienes el perfil del estudiante. NO preguntes por datos que ya figuran arriba (nivel de estudios, nombre, ubicación si está rellena). Úsalos directamente al buscar.
            1. Haz UNA sola pregunta por mensaje. Escucha antes de recomendar.
            2. CUÁNDO BUSCAR: llama a buscarFormaciones() cuando el estudiante tenga un área de interés
               concreta (ej: "informática", "cocina", "DAW", "enfermería"). Para preguntas generales
               ("¿qué estudiar?", "¿qué sectores tienen demanda?"), orienta en 2-3 líneas y luego
               pregunta por el área que más le llama la atención — no busques todavía.
            3. CÓMO BUSCAR: usa el parámetro "nombres" (array) para cubrir sinónimos y ramas del área
               en una sola llamada. Ejemplos:
               - "informática" → nombres=["informática","sistemas informáticos","redes","desarrollo","software","DAW","DAM","ASIR"]
               - "cocina" → nombres=["cocina","gastronomía","hostelería","panadería","repostería"]
               - "sanidad" → nombres=["enfermería","auxiliar de enfermería","farmacia","laboratorio clínico"]
               Luego, si la herramienta devuelve 0 resultados, amplía ronda a ronda:
               → Ronda 1: nombres[] con sinónimos + tipoEstudiosNombre + localización si aplica
               → Ronda 2: mismo nombres[], sin tipoEstudiosNombre o ampliando localización (provincia → CCAA → España)
               → Ronda 3: nombres[] ampliado o solo nombre libre, sin filtros geográficos ni de tipo
               Solo después de 3 intentos fallidos informa al estudiante de que no hay oferta en la plataforma.
            4. Cuando recomiendes una ruta → explica la secuencia completa: primero qué estudiar, luego qué habilita ese título.
            5. Menciona 2-3 salidas laborales concretas al proponer una ruta de estudios.
            6. Si necesitas un dato para afinar la búsqueda (modalidad, presupuesto, situación laboral) → pídelo con contexto, solo una vez por dato.

            ## CÓMO CITAR FORMACIONES
            La herramienta te devuelve resultados en este formato:
              [FORMACION:uuid-de-la-herramienta] "Nombre exacto"

            Al citar, copia el UUID y el nombre TAL CUAL los recibiste. Ejemplo:
              [FORMACION:a1b2c3d4-...] "Técnico en Cocina y Gastronomía"

            Nunca inventes ni modifiques un UUID. Cita todas las formaciones relevantes que devuelva la herramienta, hasta un máximo de 9.

            ## FORMATO DE RESPUESTA
            - Siempre en español. Tono cercano, natural y motivador.
            - Máximo 3-4 párrafos por respuesta. Directo y sin relleno.
            - Si después de 3 intentos de búsqueda no hay formaciones, explícalo brevemente y orienta:
              "En la plataforma no tenemos formaciones de [área] ahora mismo. Puedes buscar directamente
              en los centros de tu zona o contactar con ellos. ¿Te interesa explorar un área relacionada?"
              No sugieras formaciones de tu propia cabeza.
            - Si el estudiante pregunta algo fuera de orientación académica o profesional, di: "Soy un orientador académico y solo puedo ayudarte con formaciones y rutas de estudio. ¿En qué área te puedo orientar?"

            ## NUNCA HAGAS ESTO
            - Inventar nombres, precios, duraciones, centros o UUIDs de formaciones.
            - Decir "generalmente suele haber", "normalmente existe" o similares sobre formaciones concretas.
            - Recomendar formaciones que no estén en la plataforma EncuentraFormación.
            - Responder sobre temas ajenos a la orientación académica o profesional.
            - Preguntar al estudiante datos que ya tienes en su perfil: nombre, nivel de estudios, ubicación (si está rellena). Úsalos directamente.
            - Pedir datos personales innecesarios: solo intereses y situación laboral si son relevantes para la búsqueda.
            - Repetir la misma pregunta que ya hiciste en un turno anterior.
            - Escribir el nombre de una herramienta ni sus argumentos en el campo de texto de tu respuesta. Las llamadas a herramientas se hacen de forma interna; el usuario nunca debe ver "buscarFormaciones{...}" ni nada similar.
            """.formatted(nombre, gradoLabel, accesibles, ubicacionLine, favoritosStr, solicitudesStr);
    }

    /**
     * Mapea el nivel de estudios a formaciones accesibles con instrucciones de búsqueda para el LLM.
     */
    private String mapearAccesibles(String gradoNombre) {
        return switch (gradoNombre) {
            case "Sin estudios", "Primaria" -> """
                Acceso a: Cursos de formación no reglada y Certificados de Profesionalidad (Formación para el Empleo, SEPE/FUNDAE).
                Busca con tipoEstudiosNombre="Curso / Formación no reglada" o "Certificado de Profesionalidad".
                Vías de progresión a tener en cuenta:
                - Con 17+ años puede presentarse a la Prueba de Acceso a Ciclos Formativos de Grado Medio.
                - Puede cursar ESO para adultos (gratuita) para desbloquear más opciones.
                Oriéntale hacia cursos prácticos y certificados de profesionalidad mientras valoras si le interesa progresar.""";
            case "ESO" -> """
                Acceso directo a: FP Básica, FP Grado Medio y Bachillerato.
                También puede hacer cualquier Curso de formación no reglada o Certificado de Profesionalidad.
                Busca con tipoEstudiosNombre="FP Básica", "FP Grado Medio", "Bachillerato", "Curso / Formación no reglada" o "Certificado de Profesionalidad".
                Explícale la diferencia de rutas:
                - Bachillerato (2 años) → EvAU → Universidad o FP Grado Superior.
                - FP Grado Medio (2 años) → salida laboral directa o acceso a FP Grado Superior sin EvAU.""";
            case "Bachillerato" -> """
                Acceso directo a: FP Grado Superior y Cursos.
                Con la EvAU/EBAU superada: Grado Universitario.
                Busca con tipoEstudiosNombre="FP Grado Superior", "Grado Universitario", "Curso / Formación no reglada" o "Certificado de Profesionalidad".
                Aclara siempre que el Grado Universitario requiere superar la EvAU.
                Los Másteres Oficiales requieren tener un Grado Universitario previo; no se accede desde Bachillerato.""";
            case "FP Básica", "Grado Medio" -> """
                Acceso directo a: FP Grado Superior, con posibles convalidaciones de módulos.
                También puede hacer Cursos de formación no reglada o Certificados de Profesionalidad.
                Busca con tipoEstudiosNombre="FP Grado Superior", "Curso / Formación no reglada" o "Certificado de Profesionalidad".
                Vías adicionales:
                - Acceso a la Universidad mediante prueba de acceso para mayores de 25 años, o directamente \
                en algunos grados si el CFGM es de la misma rama.
                - Con el CFGS obtenido podrá acceder directamente a la Universidad con créditos reconocibles.""";
            case "Grado Superior" -> """
                Acceso directo a: Grado Universitario (con créditos reconocibles del CFGS) y Cursos.
                Busca con tipoEstudiosNombre="Grado Universitario", "Curso / Formación no reglada" o "Certificado de Profesionalidad".
                Importante: los Másteres Oficiales requieren un Grado Universitario completo; \
                no se puede acceder directamente desde un CFGS.""";
            case "Grado Universitario" -> """
                Acceso directo a: Máster Oficial, Máster / Título propio y Cursos de especialización.
                El Doctorado normalmente requiere un Máster Oficial previo (o equivalente acreditado).
                Busca con tipoEstudiosNombre="Máster Oficial", "Máster / Título propio", "Doctorado" o "Curso / Formación no reglada".""";
            case "Máster Oficial" -> """
                Acceso directo a: Doctorado y Cursos de especialización.
                Busca con tipoEstudiosNombre="Doctorado" o "Curso / Formación no reglada".""";
            case "Doctorado" -> """
                Puede acceder a cualquier formación de la plataforma: cursos de especialización, másteres, certificados, etc.
                Busca sin restricción de tipoEstudiosNombre o úsalo según el interés concreto del estudiante.""";
            default -> "Nivel no especificado. Pregunta al estudiante cuál es su titulación más alta para orientarle correctamente.";
        };
    }
}
