package com.irisperez.tfg.encuentraformacion.mapper.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ComunidadAutonomaDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComunidadAutonomaMapper {

    ComunidadAutonomaDTO toDTO(ComunidadAutonoma comunidadAutonoma);

    List<ComunidadAutonomaDTO> toDTOList(List<ComunidadAutonoma> comunidades);
}
