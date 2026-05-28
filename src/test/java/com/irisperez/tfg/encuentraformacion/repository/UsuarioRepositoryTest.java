package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("UsuarioRepository")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setEmail("test@ejemplo.com");
        usuario.setUsername("usuarioTest");
        usuario.setNombre("Juan");
        usuario.setApellidos("Pérez");
        usuario.setPassword("$2a$10$hashed");
        usuario.setDni("12345678A");
        usuario.setActivo(true);
        usuarioRepository.saveAndFlush(usuario);
    }

    @Test
    @DisplayName("findByEmail retorna usuario cuando existe")
    void findByEmail_CuandoExiste_RetornaUsuario() {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail("test@ejemplo.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsername()).isEqualTo("usuarioTest");
    }

    @Test
    @DisplayName("findByEmail retorna vacío cuando no existe")
    void findByEmail_CuandoNoExiste_RetornaVacio() {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail("noexiste@ejemplo.com");

        assertThat(encontrado).isEmpty();
    }

    @Test
    @DisplayName("findByActivoTrue retorna solo usuarios activos")
    void findByActivoTrue_RetornaUsuariosActivos() {
        Usuario inactivo = new Usuario();
        inactivo.setEmail("inactivo@ejemplo.com");
        inactivo.setUsername("inactivo");
        inactivo.setNombre("Ana");
        inactivo.setApellidos("Gómez");
        inactivo.setPassword("pass");
        inactivo.setActivo(false);
        usuarioRepository.saveAndFlush(inactivo);

        List<Usuario> activos = usuarioRepository.findByActivoTrue();

        assertThat(activos).hasSize(1);
        assertThat(activos.get(0).getEmail()).isEqualTo("test@ejemplo.com");
    }

    @Test
    @DisplayName("existsByEmail retorna true si existe")
    void existsByEmail_CuandoExiste_RetornaTrue() {
        boolean existe = usuarioRepository.existsByEmail("test@ejemplo.com");
        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("existsByUsername retorna true si existe")
    void existsByUsername_CuandoExiste_RetornaTrue() {
        boolean existe = usuarioRepository.existsByUsername("usuarioTest");
        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("existsByDni retorna true si existe")
    void existsByDni_CuandoExiste_RetornaTrue() {
        boolean existe = usuarioRepository.existsByDni("12345678A");
        assertThat(existe).isTrue();
    }
}
