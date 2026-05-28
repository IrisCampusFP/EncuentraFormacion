package com.irisperez.tfg.encuentraformacion.service.auth;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.RolDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.RolMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    @Autowired
    public RolService(RolRepository rolRepository, RolMapper rolMapper) {
        this.rolRepository = rolRepository;
        this.rolMapper = rolMapper;
    }

    // Obtener roles existentes
    @Transactional(readOnly = true)
    public List<Rol> obtenerRoles() {
        return rolRepository.findAll();
    }

    // Obtener todos los roles como DTO
    @Transactional(readOnly = true)
    public List<RolDTO> obtenerRolesDTO() {
        return rolMapper.toDTOList(obtenerRoles());
    }

    // Obtener roles por id
    @Transactional(readOnly = true)
    public List<Rol> obtenerRolesPorId(List<Long> idsRoles) {
        return rolRepository.findAllById(idsRoles);
    }

//    // Obtener rol por id
//    @Transactional(readOnly = true)
//    public Rol obtenerRolPorId(Long idRol) {
//        return rolRepository.findById(idRol)
//                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el rol con id: " + idRol));
//    }
//
//    // Obtener DTO rol por id
//    @Transactional(readOnly = true)
//    public RolDTO obtenerRolDTOPorId(Long idRol) {
//        return rolMapper.toDTO(obtenerRolPorId(idRol));
//    }

    // Obtener rol por nombre
    @Transactional(readOnly = true)
    public Rol obtenerRolPorNombre(RolNombre nombreRol) {
        return rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el rol con nombre: " + nombreRol));
    }

}