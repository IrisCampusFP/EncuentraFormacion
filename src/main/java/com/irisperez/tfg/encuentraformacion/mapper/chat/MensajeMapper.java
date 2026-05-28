package com.irisperez.tfg.encuentraformacion.mapper.chat;

import com.irisperez.tfg.encuentraformacion.dto.chat.MensajeChatDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Mensaje;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MensajeMapper {

    @Mapping(source = "remitente.id",     target = "remitenteId")
    @Mapping(source = "remitente.nombre", target = "remitenteNombre")
    MensajeChatDTO toDTO(Mensaje mensaje);

    List<MensajeChatDTO> toDTOList(List<Mensaje> mensajes);
}
