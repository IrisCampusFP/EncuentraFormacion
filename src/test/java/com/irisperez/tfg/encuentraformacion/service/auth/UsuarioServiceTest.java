package com.irisperez.tfg.encuentraformacion.service.auth;

import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroUsuarioEstudianteRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroRequestDTO;
import com.irisperez.tfg.encuentraformacion.mapper.usuario.UsuarioMapper;
import com.irisperez.tfg.encuentraformacion.dto.usuario.PerfilUpdateDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.entity.GradoEstudios;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import com.irisperez.tfg.encuentraformacion.repository.EstudianteRepository;
import com.irisperez.tfg.encuentraformacion.repository.GradoEstudiosRepository;
import com.irisperez.tfg.encuentraformacion.repository.ProvinciaRepository;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import com.irisperez.tfg.encuentraformacion.repository.SolicitudGestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolService rolService;
    @Mock private GradoEstudiosRepository gradoEstudiosRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ProvinciaRepository provinciaRepository;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SolicitudGestionRepository solicitudGestionRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private UsuarioDTO usuarioDTO;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("ana@test.com");
        usuario.setUsername("ana99");
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        usuario.setPassword("Password1");
        usuario.setActivo(true);
        usuario.setRoles(new ArrayList<>());

        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setId(1L);
        usuarioDTO.setEmail("ana@test.com");
        usuarioDTO.setUsername("ana99");
        usuarioDTO.setNombre("Ana");
        usuarioDTO.setApellidos("García");
        usuarioDTO.setActivo(true);
        usuarioDTO.setRoles(new ArrayList<>());
    }

    @Nested
    @DisplayName("crearUsuario()")
    class CrearUsuario {

        @Test
        @DisplayName("usuario válido guarda la contraseña hasheada y retorna DTO")
        void usuarioValido_guardaYRetornaDTO() {
            when(usuarioRepository.existsByEmail("ana@test.com")).thenReturn(false);
            when(usuarioRepository.existsByUsername("ana99")).thenReturn(false);
            when(passwordEncoder.encode("Password1")).thenReturn("$2a$hashed");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(usuarioMapper.toDTO(usuario)).thenReturn(usuarioDTO);

            UsuarioDTO resultado = usuarioService.crearUsuario(usuario);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getEmail()).isEqualTo("ana@test.com");
            verify(passwordEncoder).encode("Password1");
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("usuario nulo lanza IllegalArgumentException")
        void usuarioNulo_lanzaIllegalArgumentException() {
            assertThatThrownBy(() -> usuarioService.crearUsuario(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario nulo");
            verifyNoInteractions(usuarioRepository);
        }

        @Test
        @DisplayName("email duplicado lanza IllegalArgumentException")
        void emailDuplicado_lanzaIllegalArgumentException() {
            when(usuarioRepository.existsByEmail("ana@test.com")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crearUsuario(usuario))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un usuario con ese email");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("username duplicado lanza IllegalArgumentException")
        void usernameDuplicado_lanzaIllegalArgumentException() {
            when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(usuarioRepository.existsByUsername("ana99")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crearUsuario(usuario))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un usuario con ese username");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("DNI duplicado lanza IllegalArgumentException")
        void dniDuplicado_lanzaIllegalArgumentException() {
            usuario.setDni("12345678A");
            when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
            when(usuarioRepository.existsByDni("12345678A")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crearUsuario(usuario))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un usuario con ese dni");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("DNI en blanco se normaliza a null antes de persistir")
        void dniEnBlanco_seNormalizaANull() {
            usuario.setDni("   ");
            when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(usuarioMapper.toDTO(any(Usuario.class))).thenReturn(usuarioDTO);

            usuarioService.crearUsuario(usuario);

            assertThat(usuario.getDni()).isNull();
        }
    }

    @Nested
    @DisplayName("registrarEstudiante()")
    class RegistrarEstudiante {

        @Test
        @DisplayName("datos válidos registra al estudiante con grado y rol")
        void datosValidos_registraCorrectamente() {
            RegistroUsuarioEstudianteRequestDTO dto = new RegistroUsuarioEstudianteRequestDTO();
            dto.setGradoEstudios("Bachillerato");
            
            RegistroRequestDTO datosUsuario = new RegistroRequestDTO();
            datosUsuario.setEmail("estudiante@test.com");
            datosUsuario.setUsername("estud99");
            datosUsuario.setPassword("Pass123");
            dto.setDatosUsuario(datosUsuario);

            GradoEstudios grado = new GradoEstudios();
            grado.setNombre("Bachillerato");

            Rol rolEstudiante = new Rol();
            rolEstudiante.setNombre(RolNombre.ESTUDIANTE);

            Usuario tempUser = new Usuario();
            tempUser.setPassword("Pass123");
            tempUser.setRoles(new ArrayList<>());
            
            // Simula el flujo completo de validación y enriquecimiento de la entidad estudiante
            when(usuarioMapper.createUsuarioEstudianteFromDTO(dto)).thenReturn(tempUser);
            when(gradoEstudiosRepository.findByNombre("Bachillerato")).thenReturn(Optional.of(grado));
            when(rolService.obtenerRolPorNombre(RolNombre.ESTUDIANTE)).thenReturn(rolEstudiante);
            
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(passwordEncoder.encode("Pass123")).thenReturn("$2a$hashed");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(usuarioMapper.toDTO(any())).thenReturn(usuarioDTO);

            UsuarioDTO resultado = usuarioService.registrarEstudiante(dto);

            assertThat(resultado).isNotNull();
            assertThat(tempUser.getEstudiante()).isNotNull();
            assertThat(tempUser.getEstudiante().getGradoEstudios()).isEqualTo(grado);
            assertThat(tempUser.getRoles()).contains(rolEstudiante);
            verify(usuarioRepository).save(tempUser);
        }

        @Test
        @DisplayName("grado de estudios nulo lanza IllegalArgumentException")
        void gradoNulo_lanzaIllegalArgumentException() {
            RegistroUsuarioEstudianteRequestDTO dto = new RegistroUsuarioEstudianteRequestDTO();
            dto.setGradoEstudios(null);

            Usuario tempUser = new Usuario();

            when(usuarioMapper.createUsuarioEstudianteFromDTO(dto)).thenReturn(tempUser);

            assertThatThrownBy(() -> usuarioService.registrarEstudiante(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Debes indicar un Grado de Estudios");
        }
        
        @Test
        @DisplayName("grado de estudios inexistente lanza IllegalArgumentException")
        void gradoInexistente_lanzaIllegalArgumentException() {
            RegistroUsuarioEstudianteRequestDTO dto = new RegistroUsuarioEstudianteRequestDTO();
            dto.setGradoEstudios("Inventado");

            Usuario tempUser = new Usuario();

            when(usuarioMapper.createUsuarioEstudianteFromDTO(dto)).thenReturn(tempUser);
            when(gradoEstudiosRepository.findByNombre("Inventado")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.registrarEstudiante(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no válido");
        }

        @Test
        @DisplayName("provinciaId válido asigna la provincia al estudiante")
        void provinciaIdValido_asignaProvincia() {
            RegistroUsuarioEstudianteRequestDTO dto = new RegistroUsuarioEstudianteRequestDTO();
            dto.setGradoEstudios("ESO");
            dto.setProvinciaId(28L);

            RegistroRequestDTO datosUsuario = new RegistroRequestDTO();
            datosUsuario.setEmail("est@test.com");
            datosUsuario.setUsername("est99");
            datosUsuario.setPassword("Pass123");
            dto.setDatosUsuario(datosUsuario);

            GradoEstudios grado = new GradoEstudios(); grado.setNombre("ESO");
            Provincia provincia = new Provincia(); provincia.setId(28L); provincia.setNombre("Madrid");
            Rol rolEstudiante = new Rol(); rolEstudiante.setNombre(RolNombre.ESTUDIANTE);

            Usuario tempUser = new Usuario();
            tempUser.setPassword("Pass123");
            tempUser.setRoles(new ArrayList<>());

            when(usuarioMapper.createUsuarioEstudianteFromDTO(dto)).thenReturn(tempUser);
            when(gradoEstudiosRepository.findByNombre("ESO")).thenReturn(Optional.of(grado));
            when(provinciaRepository.findById(28L)).thenReturn(Optional.of(provincia));
            when(rolService.obtenerRolPorNombre(RolNombre.ESTUDIANTE)).thenReturn(rolEstudiante);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(usuarioMapper.toDTO(any())).thenReturn(usuarioDTO);

            usuarioService.registrarEstudiante(dto);

            assertThat(tempUser.getEstudiante().getProvincia()).isEqualTo(provincia);
        }

        @Test
        @DisplayName("provinciaId inválido lanza IllegalArgumentException")
        void provinciaIdInvalido_lanzaIllegalArgumentException() {
            RegistroUsuarioEstudianteRequestDTO dto = new RegistroUsuarioEstudianteRequestDTO();
            dto.setGradoEstudios("ESO");
            dto.setProvinciaId(999L);

            RegistroRequestDTO datosUsuario = new RegistroRequestDTO();
            datosUsuario.setEmail("est@test.com");
            datosUsuario.setUsername("est99");
            datosUsuario.setPassword("Pass123");
            dto.setDatosUsuario(datosUsuario);

            GradoEstudios grado = new GradoEstudios(); grado.setNombre("ESO");
            Usuario tempUser = new Usuario();

            when(usuarioMapper.createUsuarioEstudianteFromDTO(dto)).thenReturn(tempUser);
            when(gradoEstudiosRepository.findByNombre("ESO")).thenReturn(Optional.of(grado));
            when(provinciaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.registrarEstudiante(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Provincia no válida");
        }

        @Test
        @DisplayName("localidad se guarda trim al registrar estudiante")
        void localidad_seGuardaTrimmed() {
            RegistroUsuarioEstudianteRequestDTO dto = new RegistroUsuarioEstudianteRequestDTO();
            dto.setGradoEstudios("ESO");
            dto.setLocalidad("  Alcorcón  ");

            RegistroRequestDTO datosUsuario = new RegistroRequestDTO();
            datosUsuario.setEmail("est@test.com");
            datosUsuario.setUsername("est99");
            datosUsuario.setPassword("Pass123");
            dto.setDatosUsuario(datosUsuario);

            GradoEstudios grado = new GradoEstudios(); grado.setNombre("ESO");
            Rol rolEstudiante = new Rol(); rolEstudiante.setNombre(RolNombre.ESTUDIANTE);

            Usuario tempUser = new Usuario();
            tempUser.setPassword("Pass123");
            tempUser.setRoles(new ArrayList<>());

            when(usuarioMapper.createUsuarioEstudianteFromDTO(dto)).thenReturn(tempUser);
            when(gradoEstudiosRepository.findByNombre("ESO")).thenReturn(Optional.of(grado));
            when(rolService.obtenerRolPorNombre(RolNombre.ESTUDIANTE)).thenReturn(rolEstudiante);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(usuarioMapper.toDTO(any())).thenReturn(usuarioDTO);

            usuarioService.registrarEstudiante(dto);

            assertThat(tempUser.getEstudiante().getLocalidad()).isEqualTo("Alcorcón");
        }
    }

    @Nested
    @DisplayName("existeEmail() / existeUsername()")
    class ExistenciaUnica {

        @Test
        @DisplayName("email existente retorna true")
        void existeEmail_existente_retornaTrue() {
            when(usuarioRepository.existsByEmail("ana@test.com")).thenReturn(true);
            assertThat(usuarioService.existeEmail("ana@test.com")).isTrue();
        }

        @Test
        @DisplayName("email no registrado retorna false")
        void existeEmail_noExistente_retornaFalse() {
            when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
            assertThat(usuarioService.existeEmail("nuevo@test.com")).isFalse();
        }

        @Test
        @DisplayName("username existente retorna true")
        void existeUsername_existente_retornaTrue() {
            when(usuarioRepository.existsByUsername("ana99")).thenReturn(true);
            assertThat(usuarioService.existeUsername("ana99")).isTrue();
        }

        @Test
        @DisplayName("username no registrado retorna false")
        void existeUsername_noExistente_retornaFalse() {
            when(usuarioRepository.existsByUsername("nuevo99")).thenReturn(false);
            assertThat(usuarioService.existeUsername("nuevo99")).isFalse();
        }
    }

    @Nested
    @DisplayName("obtenerUsuarioPorEmail()")
    class ObtenerPorEmail {

        @Test
        @DisplayName("email registrado retorna la entidad")
        void emailRegistrado_retornaUsuario() {
            when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));

            Usuario resultado = usuarioService.obtenerUsuarioPorEmail("ana@test.com");

            assertThat(resultado).isNotNull();
            assertThat(resultado.getEmail()).isEqualTo("ana@test.com");
        }

        @Test
        @DisplayName("email no registrado lanza IllegalArgumentException")
        void emailNoRegistrado_lanzaIllegalArgumentException() {
            when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obtenerUsuarioPorEmail("noexiste@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe ningún usuario registrado con ese email");
        }
    }

    @Nested
    @DisplayName("obtenerUsuarioPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("id existente retorna la entidad")
        void idExistente_retornaUsuario() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            Usuario resultado = usuarioService.obtenerUsuarioPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("id inexistente lanza IllegalArgumentException")
        void idInexistente_lanzaIllegalArgumentException() {
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obtenerUsuarioPorId(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("obtenerUsuarioDTOPorEmail() / obtenerUsuarioDTOPorId() / obtenerUsuarios()")
    class MappersDeObtencion {

        @Test
        @DisplayName("obtenerUsuarioDTOPorEmail mapea correctamente la entidad a DTO")
        void obtenerDTOPorEmail_mapeaADTO() {
            when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
            when(usuarioMapper.toDTO(usuario)).thenReturn(usuarioDTO);
            when(solicitudGestionRepository.existsByUsuarioIdAndEstado(anyLong(), any())).thenReturn(false);
            
            UsuarioDTO dto = usuarioService.obtenerUsuarioDTOPorEmail("ana@test.com");
            
            assertThat(dto).isNotNull();
            assertThat(dto.getEmail()).isEqualTo("ana@test.com");
        }

        @Test
        @DisplayName("obtenerUsuarioDTOPorId mapea correctamente la entidad a DTO")
        void obtenerDTOPorId_mapeaADTO() {
            when(usuarioRepository.findByIdWithCentros(1L)).thenReturn(Optional.of(usuario));
            when(usuarioMapper.toDTO(usuario)).thenReturn(usuarioDTO);
            
            UsuarioDTO dto = usuarioService.obtenerUsuarioDTOPorId(1L);
            
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("obtenerUsuarios retorna la lista completa mapeada")
        void obtenerUsuarios_retornaListaDTO() {
            when(usuarioRepository.findAll()).thenReturn(List.of(usuario));
            when(usuarioMapper.toDTOList(List.of(usuario))).thenReturn(List.of(usuarioDTO));
            
            List<UsuarioDTO> dtoList = usuarioService.obtenerUsuarios();
            
            assertThat(dtoList).hasSize(1);
            assertThat(dtoList.get(0).getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("obtenerUsuariosPaginados()")
    class ObtenerPaginados {

        @Test
        @DisplayName("retorna página con los usuarios correctamente mapeados")
        void retornaPaginaCorrecta() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Usuario> pageEntidades = new PageImpl<>(List.of(usuario));
            when(usuarioRepository.findAll(pageable)).thenReturn(pageEntidades);
            when(usuarioMapper.toDTO(usuario)).thenReturn(usuarioDTO);

            Page<UsuarioDTO> resultado = usuarioService.obtenerUsuariosPaginados(pageable);

            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getEmail()).isEqualTo("ana@test.com");
        }
    }

    @Nested
    @DisplayName("cambiarPassword()")
    class CambiarPassword {

        @Test
        @DisplayName("datos correctos actualiza la contraseña hasheada")
        void datosCorrectos_cambiaPassword() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Password1", "Password1")).thenReturn(true);
            when(passwordEncoder.encode("NuevoPass2")).thenReturn("$2a$nuevo");
            when(usuarioRepository.save(any())).thenReturn(usuario);

            assertThatCode(() -> usuarioService.cambiarPassword(1L, "Password1", "NuevoPass2"))
                    .doesNotThrowAnyException();

            assertThat(usuario.getPassword()).isEqualTo("$2a$nuevo");
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("usuario no encontrado lanza IllegalArgumentException")
        void usuarioNoEncontrado_lanzaIllegalArgumentException() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.cambiarPassword(99L, "pass", "nueva"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("usuario desactivado lanza IllegalStateException")
        void usuarioInactivo_lanzaIllegalStateException() {
            usuario.setActivo(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.cambiarPassword(1L, "Password1", "NuevoPass2"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("desactivado");
        }

        @Test
        @DisplayName("contraseña actual incorrecta lanza IllegalArgumentException")
        void passwordActualIncorrecta_lanzaIllegalArgumentException() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.cambiarPassword(1L, "Incorrecta1", "NuevoPass2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Contraseña actual incorrecta");
        }

        @Test
        @DisplayName("contraseña nueva vacía lanza IllegalArgumentException")
        void passwordNuevaVacia_lanzaIllegalArgumentException() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.cambiarPassword(1L, "Password1", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contraseña no puede estar vacía");
        }

        @Test
        @DisplayName("contraseña nueva en blanco lanza IllegalArgumentException")
        void passwordNuevaEnBlanco_lanzaIllegalArgumentException() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.cambiarPassword(1L, "Password1", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contraseña no puede estar vacía");
        }
    }

    @Nested
    @DisplayName("cambiarPasswordAdmin()")
    class CambiarPasswordAdmin {

        @Test
        @DisplayName("datos correctos cambia la contraseña sin verificar la actual")
        void datosCorrectos_cambiaPassword() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.encode("NuevoPass2")).thenReturn("$2a$admin");
            when(usuarioRepository.save(any())).thenReturn(usuario);

            assertThatCode(() -> usuarioService.cambiarPasswordAdmin(1L, "NuevoPass2"))
                    .doesNotThrowAnyException();

            verify(passwordEncoder).encode("NuevoPass2");
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("contraseña vacía lanza IllegalArgumentException")
        void passwordVacia_lanzaIllegalArgumentException() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.cambiarPasswordAdmin(1L, "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contraseña no puede estar vacía");
        }
    }

    @Nested
    @DisplayName("actualizarUsuario()")
    class ActualizarUsuario {

        @Test
        @DisplayName("DTO nulo lanza IllegalArgumentException")
        void dtoNulo_lanzaIllegalArgumentException() {
            assertThatThrownBy(() -> usuarioService.actualizarUsuario(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No se han recibido correctamente los nuevos datos");
        }

        @Test
        @DisplayName("usuario inexistente lanza IllegalStateException")
        void usuarioNoExiste_lanzaIllegalStateException() {
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
            assertThatThrownBy(() -> usuarioService.actualizarUsuario(999L, dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("El usuario no existe en la base de datos");
        }

        @Test
        @DisplayName("sin cambios en email ni username actualiza sin verificar unicidad")
        void mismoEmailYUsername_noComprueba_actualiza() {
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
            dto.setEmail("ana@test.com");
            dto.setUsername("ana99");
            dto.setNombre("Ana Modificada");
            dto.setApellidos("García");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(usuarioMapper.toDTO(any())).thenReturn(usuarioDTO);

            assertThatCode(() -> usuarioService.actualizarUsuario(1L, dto))
                    .doesNotThrowAnyException();

            verify(usuarioRepository, never()).existsByEmail(anyString());
            verify(usuarioRepository, never()).existsByUsername(anyString());
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("email cambiado y duplicado lanza IllegalArgumentException")
        void emailCambiado_duplicado_lanzaIllegalArgumentException() {
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
            dto.setEmail("duplicado@test.com");
            dto.setUsername("ana99");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.existsByEmail("duplicado@test.com")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.actualizarUsuario(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un usuario con ese email");
        }

        @Test
        @DisplayName("DNI en blanco en actualización se normaliza a null")
        void dniEnBlanco_seNormalizaANull() {
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
            dto.setEmail("ana@test.com");
            dto.setUsername("ana99");
            dto.setDni("   ");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(usuarioMapper.toDTO(any())).thenReturn(usuarioDTO);

            usuarioService.actualizarUsuario(1L, dto);

            assertThat(dto.getDni()).isNull();
        }
    }

    @Nested
    @DisplayName("actualizarPerfil()")
    class ActualizarPerfil {

        @Test
        @DisplayName("provincia y localidad se actualizan en el estudiante")
        void provinciaYLocalidad_seActualizanEnEstudiante() {
            Provincia provincia = new Provincia(); provincia.setId(41L); provincia.setNombre("Sevilla");
            GradoEstudios grado = new GradoEstudios(); grado.setId(3L);

            Estudiante estudiante = new Estudiante();
            estudiante.setId(5L);
            estudiante.setUsuario(usuario);
            usuario.setEstudiante(estudiante);

            PerfilUpdateDTO dto = new PerfilUpdateDTO();
            dto.setUsername("ana99");
            dto.setEmail("ana@test.com");
            dto.setNombre("Ana");
            dto.setApellidos("García");
            dto.setProvinciaId(41L);
            dto.setLocalidad("Dos Hermanas");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(provinciaRepository.findById(41L)).thenReturn(Optional.of(provincia));
            when(estudianteRepository.save(any())).thenReturn(estudiante);
            when(usuarioMapper.toDTO(any())).thenReturn(usuarioDTO);

            usuarioService.actualizarPerfil(1L, dto);

            assertThat(estudiante.getProvincia()).isEqualTo(provincia);
            assertThat(estudiante.getLocalidad()).isEqualTo("Dos Hermanas");
        }

        @Test
        @DisplayName("localidad en blanco se guarda como null")
        void localidadEnBlanco_seGuardaComoNull() {
            Estudiante estudiante = new Estudiante();
            estudiante.setId(5L);
            estudiante.setUsuario(usuario);
            estudiante.setLocalidad("antigua");

            PerfilUpdateDTO dto = new PerfilUpdateDTO();
            dto.setUsername("ana99");
            dto.setEmail("ana@test.com");
            dto.setNombre("Ana");
            dto.setApellidos("García");
            dto.setLocalidad("   ");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
            when(estudianteRepository.save(any())).thenReturn(estudiante);
            when(usuarioMapper.toDTO(any())).thenReturn(usuarioDTO);

            usuarioService.actualizarPerfil(1L, dto);

            assertThat(estudiante.getLocalidad()).isNull();
        }
    }

    @Nested
    @DisplayName("actualizarRolesUsuario()")
    class ActualizarRoles {

        @Test
        @DisplayName("lista de roles nula lanza IllegalArgumentException")
        void listaRolesNula_lanzaIllegalArgumentException() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.actualizarRolesUsuario(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("al menos un rol");
        }

        @Test
        @DisplayName("lista de roles vacía lanza IllegalArgumentException")
        void listaRolesVacia_lanzaIllegalArgumentException() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.actualizarRolesUsuario(1L, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("al menos un rol");
        }

        @Test
        @DisplayName("lista válida asigna los roles y persiste")
        void listaValida_asignaRoles() {
            Rol rolAdmin = new Rol();
            rolAdmin.setId(1L);
            rolAdmin.setNombre(RolNombre.ADMIN);
            rolAdmin.setUsuarios(new HashSet<>());

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(rolService.obtenerRolesPorId(List.of(1L))).thenReturn(List.of(rolAdmin));
            when(usuarioRepository.save(any())).thenReturn(usuario);

            assertThatCode(() -> usuarioService.actualizarRolesUsuario(1L, List.of(1L)))
                    .doesNotThrowAnyException();

            verify(usuarioRepository).save(usuario);
        }
    }

    @Nested
    @DisplayName("actualizarEstado()")
    class ActualizarEstado {

        @Test
        @DisplayName("desactivar usuario cambia activo a false")
        void desactivar_activoPasaAFalse() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any())).thenReturn(usuario);

            boolean estado = usuarioService.actualizarEstado(1L, false);

            assertThat(estado).isFalse();
            assertThat(usuario.getActivo()).isFalse();
        }

        @Test
        @DisplayName("activar usuario cambia activo a true")
        void activar_activoPasaATrue() {
            usuario.setActivo(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any())).thenReturn(usuario);

            boolean estado = usuarioService.actualizarEstado(1L, true);

            assertThat(estado).isTrue();
        }

        @Test
        @DisplayName("usuario no encontrado lanza IllegalStateException")
        void usuarioNoEncontrado_lanzaIllegalStateException() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.actualizarEstado(99L, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("eliminarUsuario()")
    class EliminarUsuario {

        @Test
        @DisplayName("id existente invoca deleteById")
        void idExistente_eliminaCorrectamente() {
            when(usuarioRepository.existsById(1L)).thenReturn(true);

            assertThatCode(() -> usuarioService.eliminarUsuario(1L))
                    .doesNotThrowAnyException();

            verify(usuarioRepository).deleteById(1L);
        }

        @Test
        @DisplayName("id inexistente lanza IllegalStateException sin llamar a deleteById")
        void idInexistente_lanzaIllegalStateException() {
            when(usuarioRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.eliminarUsuario(999L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("999");

            verify(usuarioRepository, never()).deleteById(anyLong());
        }
    }
}
