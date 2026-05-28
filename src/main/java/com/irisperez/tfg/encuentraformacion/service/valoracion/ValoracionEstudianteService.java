package com.irisperez.tfg.encuentraformacion.service.valoracion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.CrearValoracionDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.EditarValoracionDTO;
import com.irisperez.tfg.encuentraformacion.dto.valoracion.ValoracionDTO;
import com.irisperez.tfg.encuentraformacion.mapper.valoracion.ValoracionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Valoracion;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.repository.projection.ValoracionStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ValoracionEstudianteService {

    private final ValoracionRepository valoracionRepository;
    private final FormacionRepository formacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ValoracionMapper valoracionMapper;

    @Transactional
    public ValoracionDTO crear(CrearValoracionDTO dto, Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudiante no encontrado"));

        Formacion formacion = formacionRepository.findByUuid(dto.getFormacionUuid())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formación no encontrada o inactiva"));

        if (valoracionRepository.existsByEstudianteIdAndFormacionId(est.getId(), formacion.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya has valorado esta formación");
        }

        Valoracion v = new Valoracion();
        v.setEstudiante(est);
        v.setFormacion(formacion);
        v.setEstrellas(dto.getEstrellas());
        v.setComentario(dto.getComentario());

        return valoracionMapper.toDTO(valoracionRepository.save(v));
    }

    @Transactional
    public ValoracionDTO editar(Long valoracionId, EditarValoracionDTO dto, Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudiante no encontrado"));

        Valoracion v = valoracionRepository.findById(valoracionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Valoración no encontrada"));

        if (!v.getEstudiante().getId().equals(est.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes editar una valoración que no es tuya");
        }

        v.setEstrellas(dto.getEstrellas());
        v.setComentario(dto.getComentario());
        v.setFechaModificacion(LocalDateTime.now());

        return valoracionMapper.toDTO(valoracionRepository.save(v));
    }

    public void cargarValoraciones(List<FormacionResumenDTO> dtos) {
        List<Long> ids = dtos.stream().map(FormacionResumenDTO::getId).toList();
        if (ids.isEmpty()) return;
        Map<Long, ValoracionStatsProjection> statsMap =
            valoracionRepository.findStatsByFormacionIds(ids).stream()
                .collect(Collectors.toMap(ValoracionStatsProjection::getFormacionId, v -> v));
        dtos.forEach(dto -> {
            ValoracionStatsProjection stats = statsMap.get(dto.getId());
            if (stats != null) {
                dto.setValoracionMedia(stats.getMedia());
                dto.setTotalValoraciones(stats.getTotal());
            }
        });
    }

    @Transactional(readOnly = true)
    public ValoracionDTO miValoracion(UUID formacionUuid, Long usuarioId) {
        Estudiante est = estudianteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudiante no encontrado"));

        return valoracionRepository.findByEstudianteIdAndFormacionUuid(est.getId(), formacionUuid)
            .map(valoracionMapper::toDTO)
            .orElse(null);
    }
}
