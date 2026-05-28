package com.irisperez.tfg.encuentraformacion.mapper.formacion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionDetalleDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FormacionMapper {

    @Mapping(source = "centro.id",             target = "centroId")
    @Mapping(source = "centro.uuid",           target = "centroUuid")
    @Mapping(source = "centro.nombreComercial", target = "centroNombre")
    @Mapping(source = "centro.localidad",       target = "centroLocalidad")
    @Mapping(source = "centro.tipo",            target = "centroTipo")
    @Mapping(target = "valoracionMedia",        ignore = true)
    @Mapping(target = "totalValoraciones",      ignore = true)
    FormacionResumenDTO toResumenDTO(Formacion formacion);

    List<FormacionResumenDTO> toResumenDTOList(List<Formacion> formaciones);

    @Mapping(source = "centro.id",              target = "centroId")
    @Mapping(source = "centro.uuid",            target = "centroUuid")
    @Mapping(source = "centro.nombreComercial", target = "centroNombre")
    @Mapping(source = "centro.localidad",       target = "centroLocalidad")
    @Mapping(source = "centro.provincia.nombre",target = "centroProvincia")
    @Mapping(source = "centro.verificado",      target = "centroVerificado")
    @Mapping(source = "centro.descripcion",     target = "centroDescripcion")
    @Mapping(source = "centro.direccion",       target = "centroDireccion")
    @Mapping(source = "centro.telefono",        target = "centroTelefono")
    @Mapping(source = "centro.email",           target = "centroEmail")
    @Mapping(source = "centro.paginaWeb",       target = "centroPaginaWeb")
    @Mapping(source = "centro.tipo",             target = "centroTipo")
    @Mapping(source = "centro.tieneGestor",      target = "centroTieneGestor")
    @Mapping(target = "valoraciones",            ignore = true)
    @Mapping(target = "valoracionMedia",         ignore = true)
    @Mapping(target = "totalValoraciones",       ignore = true)
    FormacionDetalleDTO toDetalleDTO(Formacion formacion);
}
