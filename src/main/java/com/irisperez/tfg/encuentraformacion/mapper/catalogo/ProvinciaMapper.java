package com.irisperez.tfg.encuentraformacion.mapper.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.ProvinciaDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProvinciaMapper {

    @Mapping(source = "comunidadAutonoma.id",     target = "comunidadAutonomaId")
    @Mapping(source = "comunidadAutonoma.nombre",  target = "comunidadAutonoma")
    ProvinciaDTO toDTO(Provincia provincia);

    List<ProvinciaDTO> toDTOList(List<Provincia> provincias);
}
