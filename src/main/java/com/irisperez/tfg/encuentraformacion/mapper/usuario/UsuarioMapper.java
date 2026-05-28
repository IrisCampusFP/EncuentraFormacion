package com.irisperez.tfg.encuentraformacion.mapper.usuario;

import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioUpdateDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.RolMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroUsuarioEstudianteRequestDTO;

// 'uses' indica a MapStruct que use RolMapper para resolver el campo 'roles' (List<Rol> -> List<RolDTO>)
@Mapper(componentModel = "spring", uses = RolMapper.class)
public interface UsuarioMapper {

    //  Crea un Usuario a partir de un RegistroRequestDTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ultimaConexion", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    @Mapping(target = "fechaAlta", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "solicitudesGestion", ignore = true)
    @Mapping(target = "centrosGestionados", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    Usuario createUsuarioFromDTO(RegistroRequestDTO dto);

    // Crea un Usuario a partir de un RegistroUsuarioEstudianteRequestDTO
    // ("." copia todos los campos de datosUsuario al objeto Usuario)
    @Mapping(target = ".", source = "datosUsuario")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ultimaConexion", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    @Mapping(target = "fechaAlta", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "solicitudesGestion", ignore = true)
    @Mapping(target = "centrosGestionados", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    Usuario createUsuarioEstudianteFromDTO(RegistroUsuarioEstudianteRequestDTO dto);

    // Convierte un Usuario a UsuarioDTO (MapStruct mapea automáticamente los campos)
    @Mapping(target = "tieneSolicitudGestionPendiente", ignore = true)
    @Mapping(target = "gradoEstudiosId", source = "estudiante.gradoEstudios.id")
    @Mapping(target = "gradoEstudiosNombre", source = "estudiante.gradoEstudios.nombre")
    @Mapping(target = "provinciaId", source = "estudiante.provincia.id")
    @Mapping(target = "provinciaNombre", source = "estudiante.provincia.nombre")
    @Mapping(target = "localidad", source = "estudiante.localidad")
    @Mapping(target = "centrosGestionados", expression = "java(centrosToResumen(usuario.getCentrosGestionados()))")
    UsuarioDTO toDTO(Usuario usuario);

    default UsuarioDTO.CentroResumenDTO centroToResumen(Centro centro) {
        if (centro == null) return null;
        return new UsuarioDTO.CentroResumenDTO(centro.getId(), centro.getNombreComercial());
    }

    default Set<UsuarioDTO.CentroResumenDTO> centrosToResumen(Set<Centro> centros) {
        if (centros == null) return new java.util.HashSet<>();
        return centros.stream().map(this::centroToResumen).collect(java.util.stream.Collectors.toSet());
    }

    // Convierte una lista de Usuarios a una lista de UsuarioDTOs
    List<UsuarioDTO> toDTOList(List<Usuario> usuarios);

    // Actualiza una entidad Usuario existente con los datos de un UsuarioUpdateDTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "ultimaConexion", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    @Mapping(target = "fechaAlta", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "solicitudesGestion", ignore = true)
    @Mapping(target = "centrosGestionados", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    void updateUsuarioFromDTO(UsuarioUpdateDTO dto, @MappingTarget Usuario usuarioExistente);

}

