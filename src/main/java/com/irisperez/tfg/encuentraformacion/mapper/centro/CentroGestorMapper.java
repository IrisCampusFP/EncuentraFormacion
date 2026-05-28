package com.irisperez.tfg.encuentraformacion.mapper.centro;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroGestorDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CentroGestorMapper {
    @Mapping(source = "provincia.nombre", target = "provincia")
    CentroGestorDTO toDTO(Centro centro);
}
