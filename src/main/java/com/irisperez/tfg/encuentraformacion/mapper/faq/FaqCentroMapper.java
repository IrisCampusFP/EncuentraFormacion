package com.irisperez.tfg.encuentraformacion.mapper.faq;

import com.irisperez.tfg.encuentraformacion.dto.faq.FaqCentroDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.FaqCentro;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FaqCentroMapper {

    FaqCentroDTO toDTO(FaqCentro faqCentro);

    List<FaqCentroDTO> toDTOList(List<FaqCentro> faqs);
}
