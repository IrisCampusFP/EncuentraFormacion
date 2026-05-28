package com.irisperez.tfg.encuentraformacion.service.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ProvinciaDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.ProvinciaMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.repository.ProvinciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProvinciaService {

    private final ProvinciaRepository provinciaRepository;
    private final ProvinciaMapper provinciaMapper;

    public List<ProvinciaDTO> listar() {
        return provinciaMapper.toDTOList(
            provinciaRepository.findAll(Sort.by("nombre"))
        );
    }

    public Provincia buscarPorNombre(String nombre) {
        return provinciaRepository.findByNombreIgnoreCase(nombre)
            .orElseThrow(() -> new IllegalArgumentException("Provincia no encontrada: " + nombre));
    }
}
