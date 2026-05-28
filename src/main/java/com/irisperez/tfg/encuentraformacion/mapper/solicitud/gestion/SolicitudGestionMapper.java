package com.irisperez.tfg.encuentraformacion.mapper.solicitud.gestion;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestionDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudGestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SolicitudGestionMapper {

    // Convierte un SolicitudGestion a SolicitudGestionDTO.
    /* NOTA:
     * Como el DTO contiene objetos que están anidados en entidades (Usuario y Centro),
     * usamos @Mapping para decirle a MapStruct de dónde sacar el dato y dónde ponerlo.
     *  - source: ruta en la entidad origen (ej: solicitud.getUsuario().getNombre() -> "usuario.nombre")
     *  - target: nombre del campo destino en el DTO (ej: dto.setNombre() -> "nombre")
     */
    @Mapping(source = "usuario.id",              target = "idUsuario")
    @Mapping(source = "usuario.nombre",          target = "nombre")
    @Mapping(source = "usuario.apellidos",       target = "apellidos")
    @Mapping(source = "centro.id",               target = "idCentro")
    @Mapping(source = "centro.nombreComercial",  target = "nombreCentro")
    @Mapping(source = "centro.verificado",       target = "verificadoCentro")
    SolicitudGestionDTO toDTO(SolicitudGestion solicitud);

    // Convierte una lista de SolicitudGestion a una lista de SolicitudGestionDTOs
    List<SolicitudGestionDTO> toDTOList(List<SolicitudGestion> solicitudes);

}
