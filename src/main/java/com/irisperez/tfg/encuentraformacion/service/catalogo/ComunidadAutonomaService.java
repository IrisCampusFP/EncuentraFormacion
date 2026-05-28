package com.irisperez.tfg.encuentraformacion.service.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ComunidadAutonomaDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.ComunidadAutonomaMapper;
import com.irisperez.tfg.encuentraformacion.repository.ComunidadAutonomaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComunidadAutonomaService {

    private final ComunidadAutonomaRepository comunidadAutonomaRepository;
    private final ComunidadAutonomaMapper comunidadAutonomaMapper;

    public List<ComunidadAutonomaDTO> listar() {
        return comunidadAutonomaMapper.toDTOList(
            comunidadAutonomaRepository.findAll(Sort.by("nombre"))
        );
    }
}
