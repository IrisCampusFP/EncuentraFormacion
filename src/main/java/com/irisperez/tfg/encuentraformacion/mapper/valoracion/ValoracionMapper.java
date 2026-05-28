package com.irisperez.tfg.encuentraformacion.mapper.valoracion;

import com.irisperez.tfg.encuentraformacion.dto.valoracion.ValoracionDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Valoracion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ValoracionMapper {

    @Mapping(source = "estudiante.usuario.nombre", target = "nombreEstudiante")
    ValoracionDTO toDTO(Valoracion valoracion);

    List<ValoracionDTO> toDTOList(List<Valoracion> valoraciones);
}
