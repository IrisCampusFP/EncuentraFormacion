package com.irisperez.tfg.encuentraformacion.service.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.GradoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.GradoEstudiosMapper;
import com.irisperez.tfg.encuentraformacion.repository.GradoEstudiosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradoEstudiosService {

    private final GradoEstudiosRepository gradoEstudiosRepository;
    private final GradoEstudiosMapper gradoEstudiosMapper;

    // Devuelve todos los tipos de estudios disponibles para los filtros del buscador
    public List<GradoEstudiosDTO> listar() {
        return gradoEstudiosMapper.toDTOList(gradoEstudiosRepository.findAll());
    }
}
