package com.irisperez.tfg.encuentraformacion.mapper.solicitud.gestion;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestorDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudFormacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SolicitudGestorMapper {

    @Mapping(source = "estudiante.usuario.nombre", target = "estudianteNombre")
    @Mapping(source = "estudiante.usuario.apellidos", target = "estudianteApellidos")
    @Mapping(source = "estudiante.usuario.email", target = "estudianteEmail")
    @Mapping(source = "formacion.nombre", target = "formacionNombre")
    @Mapping(source = "fechaSolicitud", target = "fechaCreacion")
    @Mapping(source = "fechaRespuesta", target = "fechaResolucion")
    SolicitudGestorDTO toDTO(SolicitudFormacion solicitud);
}
