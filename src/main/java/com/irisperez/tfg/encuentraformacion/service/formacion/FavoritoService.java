package com.irisperez.tfg.encuentraformacion.service.formacion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.mapper.formacion.FormacionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.FormacionFavorita;
import com.irisperez.tfg.encuentraformacion.repository.*;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final EstudianteRepository estudianteRepository;
    private final FormacionRepository formacionRepository;
    private final FormacionFavoritaRepository favoritoRepository;
    private final FormacionMapper formacionMapper;
    private final ValoracionEstudianteService valoracionService;

    @Transactional(readOnly = true)
    public Page<FormacionResumenDTO> getMisFavoritos(Long usuarioId, Pageable pageable) {
        Estudiante est = getEstudiante(usuarioId);
        Page<FormacionResumenDTO> page = favoritoRepository.findByEstudianteId(est.getId(), pageable)
                .map(ff -> formacionMapper.toResumenDTO(ff.getFormacion()));
        valoracionService.cargarValoraciones(page.getContent());
        return page;
    }

    @Transactional(readOnly = true)
    public boolean esGuardada(UUID formacionUuid, Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);
        return favoritoRepository.existsByEstudianteIdAndFormacionUuid(est.getId(), formacionUuid);
    }

    @Transactional
    public void agregar(UUID formacionUuid, Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);
        if (favoritoRepository.existsByEstudianteIdAndFormacionUuid(est.getId(), formacionUuid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La formación ya está en favoritos");
        }
        Formacion formacion = formacionRepository.findByUuid(formacionUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formación no encontrada"));
        favoritoRepository.save(new FormacionFavorita(est, formacion));
    }

    @Transactional
    public void quitar(UUID formacionUuid, Long usuarioId) {
        Estudiante est = getEstudiante(usuarioId);
        favoritoRepository.deleteByEstudianteIdAndFormacionUuid(est.getId(), formacionUuid);
    }

    private Estudiante getEstudiante(Long usuarioId) {
        return estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de estudiante no encontrado"));
    }
}
