package com.irisperez.tfg.encuentraformacion.service.auth;

import com.irisperez.tfg.encuentraformacion.dto.catalogo.RolDTO;
import com.irisperez.tfg.encuentraformacion.mapper.catalogo.RolMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.repository.RolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolService")
class RolServiceTest {

    @Mock private RolRepository rolRepository;
    @Mock private RolMapper rolMapper;
    @InjectMocks private RolService service;

    private Rol rolEstudiante() {
        Rol r = new Rol(); r.setId(1L); r.setNombre(RolNombre.ESTUDIANTE);
        return r;
    }

    @Nested
    @DisplayName("obtenerRoles()")
    class ObtenerRolesTests {
        @Test
        @DisplayName("devuelve todos los roles")
        void obtenerRoles_ok() {
            when(rolRepository.findAll()).thenReturn(List.of(rolEstudiante()));

            assertThat(service.obtenerRoles()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("obtenerRolesDTO()")
    class ObtenerRolesDTOTests {
        @Test
        @DisplayName("devuelve lista de DTOs")
        void obtenerRolesDTO_ok() {
            Rol rol = rolEstudiante();
            RolDTO dto = new RolDTO(1L, RolNombre.ESTUDIANTE);
            when(rolRepository.findAll()).thenReturn(List.of(rol));
            when(rolMapper.toDTOList(List.of(rol))).thenReturn(List.of(dto));

            List<RolDTO> result = service.obtenerRolesDTO();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNombre()).isEqualTo(RolNombre.ESTUDIANTE);
        }
    }

    @Nested
    @DisplayName("obtenerRolesPorId()")
    class ObtenerRolesPorIdTests {
        @Test
        @DisplayName("devuelve roles que coinciden con los ids")
        void obtenerRolesPorId_ok() {
            Rol rol = rolEstudiante();
            when(rolRepository.findAllById(List.of(1L))).thenReturn(List.of(rol));

            assertThat(service.obtenerRolesPorId(List.of(1L))).hasSize(1);
        }
    }

    @Nested
    @DisplayName("obtenerRolPorNombre()")
    class ObtenerRolPorNombreTests {
        @Test
        @DisplayName("encuentra rol existente")
        void obtenerRolPorNombre_ok() {
            Rol rol = rolEstudiante();
            when(rolRepository.findByNombre(RolNombre.ESTUDIANTE)).thenReturn(Optional.of(rol));

            assertThat(service.obtenerRolPorNombre(RolNombre.ESTUDIANTE).getNombre())
                .isEqualTo(RolNombre.ESTUDIANTE);
        }

        @Test
        @DisplayName("lanza excepción si el rol no existe")
        void obtenerRolPorNombre_noExiste() {
            when(rolRepository.findByNombre(RolNombre.ADMIN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerRolPorNombre(RolNombre.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
