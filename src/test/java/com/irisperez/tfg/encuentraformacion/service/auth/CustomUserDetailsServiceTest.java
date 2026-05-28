package com.irisperez.tfg.encuentraformacion.service.auth;

import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private CustomUserDetailsService service;

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsername {

        @Test
        @DisplayName("email registrado retorna CustomUserDetails con los datos del usuario")
        void emailRegistrado_retornaCustomUserDetails() {
            Usuario usuario = new Usuario();
            usuario.setId(1L);
            usuario.setEmail("ana@test.com");
            usuario.setPassword("$2a$hashed");
            usuario.setActivo(true);
            Rol rol = new Rol();
            rol.setNombre(RolNombre.ESTUDIANTE);
            rol.setUsuarios(new HashSet<>());
            usuario.setRoles(new ArrayList<>(List.of(rol)));
            usuario.setCentrosGestionados(new HashSet<>());

            when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));

            UserDetails resultado = service.loadUserByUsername("ana@test.com");

            assertThat(resultado).isInstanceOf(CustomUserDetails.class);
            assertThat(resultado.getUsername()).isEqualTo("ana@test.com");
            assertThat(resultado.getAuthorities()).anyMatch(a -> a.getAuthority().contains("ESTUDIANTE"));
        }

        @Test
        @DisplayName("email no registrado lanza UsernameNotFoundException")
        void emailNoRegistrado_lanzaUsernameNotFoundException() {
            when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("noexiste@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("noexiste@test.com");
        }
    }
}
