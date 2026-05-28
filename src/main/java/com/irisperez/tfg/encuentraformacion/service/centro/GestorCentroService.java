package com.irisperez.tfg.encuentraformacion.service.centro;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.EditarCentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.mapper.centro.CentroGestorMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GestorCentroService {

    private final CentroRepository centroRepository;
    private final CentroGestorMapper centroGestorMapper;

    @Transactional(readOnly = true)
    public CentroGestorDTO getCentro(Long usuarioId) {
        return centroGestorMapper.toDTO(obtenerCentroDelGestor(usuarioId));
    }

    @Transactional
    public CentroGestorDTO editarCentro(Long usuarioId, EditarCentroGestorDTO dto) {
        Centro centro = obtenerCentroDelGestor(usuarioId);
        if (dto.getDescripcion() != null) centro.setDescripcion(dto.getDescripcion());
        if (dto.getDireccion() != null) centro.setDireccion(dto.getDireccion());
        if (dto.getLocalidad() != null) centro.setLocalidad(dto.getLocalidad());
        if (dto.getTipo() != null) centro.setTipo(dto.getTipo());
        if (dto.getTelefono() != null) centro.setTelefono(dto.getTelefono());
        if (dto.getEmail() != null) centro.setEmail(dto.getEmail());
        if (dto.getPaginaWeb() != null) centro.setPaginaWeb(dto.getPaginaWeb());
        return centroGestorMapper.toDTO(centroRepository.save(centro));
    }

    private Centro obtenerCentroDelGestor(Long usuarioId) {
        return centroRepository.findByGestorId(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No gestionas ningún centro"));
    }
}
