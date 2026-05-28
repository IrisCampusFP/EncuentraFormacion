package com.irisperez.tfg.encuentraformacion.mapper.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.TipoEstudiosDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.TipoEstudios;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipoEstudiosMapper {

    TipoEstudiosDTO toDTO(TipoEstudios tipoEstudios);

    List<TipoEstudiosDTO> toDTOList(List<TipoEstudios> list);
}
