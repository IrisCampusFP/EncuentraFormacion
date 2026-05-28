package com.irisperez.tfg.encuentraformacion.mapper.faq;

import com.irisperez.tfg.encuentraformacion.dto.faq.FaqGestorDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.FaqCentro;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FaqGestorMapper {
    FaqGestorDTO toDTO(FaqCentro faq);
}
