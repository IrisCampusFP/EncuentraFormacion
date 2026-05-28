package com.irisperez.tfg.encuentraformacion.service.formacion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.CrearFormacionDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.EditarFormacionDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.mapper.formacion.FormacionGestorMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.TipoEstudios;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.FormacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.ConversacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.TipoEstudiosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GestorFormacionService {

    private final FormacionRepository formacionRepository;
    private final CentroRepository centroRepository;
    private final TipoEstudiosRepository tipoEstudiosRepository;
    private final ConversacionRepository conversacionRepository;
    private final FormacionGestorMapper formacionGestorMapper;

    @Transactional(readOnly = true)
    public Page<FormacionGestorDTO> getFormaciones(Long usuarioId, Pageable pageable) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        return formacionRepository.findByCentroId(centro.getId(), pageable)
            .map(f -> {
                FormacionGestorDTO dto = formacionGestorMapper.toDTO(f);
                dto.setSolicitudesPendientes(formacionRepository.countSolicitudesPendientesByFormacionId(f.getId()));
                dto.setChatsActivos(conversacionRepository.countByFormacionId(f.getId()));
                return dto;
            });
    }

    @Transactional
    public FormacionGestorDTO crear(Long usuarioId, CrearFormacionDTO dto) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        TipoEstudios tipoEstudios = resolverTipoEstudios(dto.getTipoEstudiosId());

        Formacion f = new Formacion();
        f.setCentro(centro);
        f.setNombre(dto.getNombre());
        f.setTipoEstudios(tipoEstudios);
        f.setHorario(dto.getHorario());
        f.setModalidad(dto.getModalidad());
        f.setDescripcion(dto.getDescripcion());
        f.setTituloOficial(dto.getTituloOficial());
        f.setDuracionHoras(dto.getDuracionHoras());
        f.setPrecio(dto.getPrecio());
        f.setFechaInicio(dto.getFechaInicio());
        f.setFechaFin(dto.getFechaFin());

        FormacionGestorDTO result = formacionGestorMapper.toDTO(formacionRepository.save(f));
        result.setSolicitudesPendientes(0);
        return result;
    }

    @Transactional
    public FormacionGestorDTO editar(Long usuarioId, Long formacionId, EditarFormacionDTO dto) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        Formacion f = formacionRepository.findByIdAndCentroId(formacionId, centro.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formación no encontrada"));

        if (dto.getNombre() != null) f.setNombre(dto.getNombre());
        if (dto.getTipoEstudiosId() != null) f.setTipoEstudios(resolverTipoEstudios(dto.getTipoEstudiosId()));
        if (dto.getHorario() != null) f.setHorario(dto.getHorario());
        if (dto.getModalidad() != null) f.setModalidad(dto.getModalidad());
        if (dto.getDescripcion() != null) f.setDescripcion(dto.getDescripcion());
        if (dto.getTituloOficial() != null) f.setTituloOficial(dto.getTituloOficial());
        if (dto.getDuracionHoras() != null) f.setDuracionHoras(dto.getDuracionHoras());
        if (dto.getPrecio() != null) f.setPrecio(dto.getPrecio());
        if (dto.getFechaInicio() != null) f.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaFin() != null) f.setFechaFin(dto.getFechaFin());
        if (dto.getActiva() != null) f.setActiva(dto.getActiva());

        FormacionGestorDTO result = formacionGestorMapper.toDTO(formacionRepository.save(f));
        result.setSolicitudesPendientes(formacionRepository.countSolicitudesPendientesByFormacionId(f.getId()));
        return result;
    }

    @Transactional
    public void desactivar(Long usuarioId, Long formacionId) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        Formacion f = formacionRepository.findByIdAndCentroId(formacionId, centro.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formación no encontrada"));
        f.setActiva(false);
        formacionRepository.save(f);
    }

    private TipoEstudios resolverTipoEstudios(Long id) {
        return tipoEstudiosRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de estudios no válido"));
    }

    private Centro obtenerCentroDelGestor(Long usuarioId) {
        return centroRepository.findByGestorId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No gestionas ningún centro"));
    }
}
