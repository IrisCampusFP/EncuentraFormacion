package com.irisperez.tfg.encuentraformacion.mapper.solicitud.formacion;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.SolicitudResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudFormacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SolicitudFormacionMapper {

    @Mapping(source = "formacion.id",                    target = "formacionId")
    @Mapping(source = "formacion.uuid",                  target = "formacionUuid")
    @Mapping(source = "formacion.nombre",                target = "formacionNombre")
    @Mapping(source = "formacion.centro.id",             target = "centroId")
    @Mapping(source = "formacion.centro.uuid",           target = "centroUuid")
    @Mapping(source = "formacion.centro.nombreComercial", target = "centroNombre")
    SolicitudResumenDTO toDTO(SolicitudFormacion solicitud);
}
