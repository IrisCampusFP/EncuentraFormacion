package com.irisperez.tfg.encuentraformacion.mapper.catalogo;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.RolDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RolMapper {

    // Convierte un Rol a RolDTO (MapStruct mapea automáticamente los campos)
    RolDTO toDTO(Rol rol);
    
    // Convierte una lista de Rol a una lista de RolDTOs
    List<RolDTO> toDTOList(List<Rol> roles);

}
