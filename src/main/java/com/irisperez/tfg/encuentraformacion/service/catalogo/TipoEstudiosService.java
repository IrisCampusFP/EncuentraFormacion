package com.irisperez.tfg.encuentraformacion.service.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.TipoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.TipoEstudiosMapper;
import com.irisperez.tfg.encuentraformacion.repository.TipoEstudiosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TipoEstudiosService {

    private final TipoEstudiosRepository tipoEstudiosRepository;
    private final TipoEstudiosMapper tipoEstudiosMapper;

    public List<TipoEstudiosDTO> listar() {
        return tipoEstudiosMapper.toDTOList(tipoEstudiosRepository.findAll());
    }
}
