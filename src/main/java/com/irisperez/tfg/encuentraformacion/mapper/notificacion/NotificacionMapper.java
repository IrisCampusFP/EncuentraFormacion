package com.irisperez.tfg.encuentraformacion.mapper.notificacion;

import com.irisperez.tfg.encuentraformacion.dto.notificacion.NotificacionDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Notificacion;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificacionMapper {

    NotificacionDTO toDTO(Notificacion notificacion);

    List<NotificacionDTO> toDTOList(List<Notificacion> notificaciones);
}
