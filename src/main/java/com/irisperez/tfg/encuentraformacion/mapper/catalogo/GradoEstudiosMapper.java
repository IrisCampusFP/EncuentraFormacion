package com.irisperez.tfg.encuentraformacion.mapper.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.GradoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.GradoEstudios;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GradoEstudiosMapper {

    GradoEstudiosDTO toDTO(GradoEstudios gradoEstudios);

    List<GradoEstudiosDTO> toDTOList(List<GradoEstudios> list);
}
