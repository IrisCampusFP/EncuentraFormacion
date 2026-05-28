package com.irisperez.tfg.encuentraformacion.mapper.centro;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CentroMapper {

    // provincia es un objeto Provincia en la entidad, pero en el DTO es un String con el nombre
    @Mapping(source = "provincia.nombre", target = "provincia")
    CentroDTO toDTO(Centro centro);

    @Mapping(source = "provincia.nombre", target = "provincia")
    List<CentroDTO> toDTOList(List<Centro> centros);

    // provincia se resuelve manualmente en el servicio (requiere lookup en BD)
    @Mapping(target = "provincia", ignore = true)
    Centro createCentroFromDTO(RegistroCentroRequestDTO dto);

    @Mapping(target = "provincia", ignore = true)
    void updateCentroFromDTO(CentroUpdateDTO dto, @MappingTarget Centro centroExistente);
}
