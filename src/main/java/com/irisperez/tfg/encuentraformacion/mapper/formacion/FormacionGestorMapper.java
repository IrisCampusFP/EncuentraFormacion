package com.irisperez.tfg.encuentraformacion.mapper.formacion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionGestorDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FormacionGestorMapper {

    @Mapping(source = "tipoEstudios.nombre", target = "tipoEstudiosNombre")
    @Mapping(source = "tipoEstudios.id", target = "tipoEstudiosId")
    @Mapping(target = "solicitudesPendientes", ignore = true)
    FormacionGestorDTO toDTO(Formacion formacion);
}
