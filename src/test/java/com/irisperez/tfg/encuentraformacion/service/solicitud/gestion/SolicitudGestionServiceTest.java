package com.irisperez.tfg.encuentraformacion.service.solicitud.gestion;

import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroGestorRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestionDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.irisperez.tfg.encuentraformacion.mapper.centro.CentroMapper;
import com.irisperez.tfg.encuentraformacion.mapper.solicitud.gestion.SolicitudGestionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudGestion;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.RolRepository;
import com.irisperez.tfg.encuentraformacion.repository.SolicitudGestionRepository;
import com.irisperez.tfg.encuentraformacion.repository.UsuarioRepository;
import com.irisperez.tfg.encuentraformacion.service.auth.UsuarioService;
import com.irisperez.tfg.encuentraformacion.service.centro.CentroService;
import org.springframework.web.multipart.MultipartFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitudGestionService")
class SolicitudGestionServiceTest {

    @Mock private SolicitudGestionRepository solicitudGestionRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private CentroService centroService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CentroRepository centroRepository;
    @Mock private RolRepository rolRepository;
    @Mock private SolicitudGestionMapper solicitudGestionMapper;
    @Mock private CentroMapper centroMapper;

    @InjectMocks
    private SolicitudGestionService solicitudGestionService;

    private Usuario usuario;
    private Centro centro;
    private SolicitudGestion solicitudPendiente;
    private SolicitudGestionDTO solicitudDTO;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("gestor@test.com");
        usuario.setUsername("gestor99");
        usuario.setActivo(true);
        usuario.setRoles(new ArrayList<>());
        usuario.setCentrosGestionados(new HashSet<>());

        centro = new Centro();
        centro.setId(10L);
        centro.setCodigo("28000001");
        centro.setNombreComercial("Academia TestFP");
        centro.setVerificado(true);
        centro.setTieneGestor(false);
        centro.setGestores(new HashSet<>());
        centro.setFormaciones(new HashSet<>());
        centro.setSolicitudesGestion(new HashSet<>());

        solicitudPendiente = new SolicitudGestion();
        solicitudPendiente.setId(100L);
        solicitudPendiente.setUsuario(usuario);
        solicitudPendiente.setCentro(centro);
        solicitudPendiente.setEstado(EstadoSolicitud.PENDIENTE);
        solicitudPendiente.setFechaSolicitud(LocalDateTime.now().minusDays(1));
        solicitudPendiente.setPruebaTitularidad(new byte[]{1, 2, 3});

        solicitudDTO = new SolicitudGestionDTO();
        solicitudDTO.setId(100L);
        solicitudDTO.setEstado(EstadoSolicitud.PENDIENTE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void simularUsuarioAutenticado(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(email);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Nested
    @DisplayName("obtenerSolicitudesPendientes()")
    class ObtenerPendientes {

        @Test
        @DisplayName("retorna lista de solicitudes con estado Pendiente")
        void retornaListaPendientes() {
            when(solicitudGestionRepository.findByEstado(EstadoSolicitud.PENDIENTE))
                    .thenReturn(List.of(solicitudPendiente));
            when(solicitudGestionMapper.toDTOList(List.of(solicitudPendiente)))
                    .thenReturn(List.of(solicitudDTO));

            List<SolicitudGestionDTO> resultado = solicitudGestionService.obtenerSolicitudesPendientes();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        }
    }

    @Nested
    @DisplayName("obtenerSolicitudesProcesadas()")
    class ObtenerProcesadas {

        @Test
        @DisplayName("retorna historial de solicitudes no pendientes ordenado")
        void retornaHistorial() {
            SolicitudGestion aprobada = new SolicitudGestion();
            aprobada.setId(101L);
            aprobada.setEstado(EstadoSolicitud.ACEPTADA);

            SolicitudGestionDTO aprobadaDTO = new SolicitudGestionDTO();
            aprobadaDTO.setId(101L);
            aprobadaDTO.setEstado(EstadoSolicitud.ACEPTADA);

            when(solicitudGestionRepository.obtenerHistorialSolicitudes(EstadoSolicitud.PENDIENTE))
                    .thenReturn(List.of(aprobada));
            when(solicitudGestionMapper.toDTOList(List.of(aprobada)))
                    .thenReturn(List.of(aprobadaDTO));

            List<SolicitudGestionDTO> resultado = solicitudGestionService.obtenerSolicitudesProcesadas();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        }
    }

    @Nested
    @DisplayName("obtenerSolicitudDelUsuario()")
    class ObtenerSolicitudDelUsuario {

        @Test
        @DisplayName("usuario con solicitud pendiente retorna DTO")
        void usuarioConSolicitudPendiente_retornaDTO() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario)).thenReturn(solicitudPendiente);
            when(solicitudGestionMapper.toDTO(solicitudPendiente)).thenReturn(solicitudDTO);

            SolicitudGestionDTO resultado = solicitudGestionService.obtenerSolicitudDelUsuario();

            assertThat(resultado).isNotNull();
            assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        }

        @Test
        @DisplayName("solicitud ya procesada lanza IllegalStateException")
        void solicitudNoPendiente_lanzaIllegalStateException() {
            solicitudPendiente.setEstado(EstadoSolicitud.ACEPTADA);
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario)).thenReturn(solicitudPendiente);

            assertThatThrownBy(() -> solicitudGestionService.obtenerSolicitudDelUsuario())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ninguna solicitud pendiente");
        }

        @Test
        @DisplayName("sin solicitud lanza IllegalStateException")
        void sinSolicitud_lanzaIllegalStateException() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario)).thenReturn(null);

            assertThatThrownBy(() -> solicitudGestionService.obtenerSolicitudDelUsuario())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ninguna solicitud pendiente");
        }
    }

    @Nested
    @DisplayName("obtenerPruebaTitularidad()")
    class ObtenerPrueba {

        @Test
        @DisplayName("id existente retorna los bytes")
        void idExistente_retornaBytes() {
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));

            byte[] resultado = solicitudGestionService.obtenerPruebaTitularidad(100L);

            assertThat(resultado).isEqualTo(new byte[]{1, 2, 3});
        }

        @Test
        @DisplayName("id inexistente lanza IllegalArgumentException")
        void idInexistente_lanzaIllegalArgumentException() {
            when(solicitudGestionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudGestionService.obtenerPruebaTitularidad(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("aprobarSolicitud()")
    class AprobarSolicitud {

        @Test
        @DisplayName("solicitud válida asigna rol gestor, vincula centro y aprueba")
        void pendienteYVerificado_sinRolGestor_aprueba() {
            Rol rolGestorBD = new Rol();
            rolGestorBD.setId(2L);
            rolGestorBD.setNombre(RolNombre.GESTOR_CENTRO);
            rolGestorBD.setUsuarios(new HashSet<>());

            // Simula la recuperación de la base de datos y la inyección del rol GESTOR_CENTRO antes de aprobar
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));
            when(rolRepository.findByNombre(RolNombre.GESTOR_CENTRO)).thenReturn(Optional.of(rolGestorBD));
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(centroRepository.save(any())).thenReturn(centro);
            when(solicitudGestionRepository.save(any())).thenReturn(solicitudPendiente);
            when(solicitudGestionMapper.toDTO(any())).thenReturn(solicitudDTO);

            SolicitudGestionDTO resultado = solicitudGestionService.aprobarSolicitud(100L);

            assertThat(resultado).isNotNull();
            assertThat(solicitudPendiente.getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);
            assertThat(solicitudPendiente.getFechaResolucion()).isNotNull();
            assertThat(centro.getTieneGestor()).isTrue();
            assertThat(usuario.getCentrosGestionados()).contains(centro);
            assertThat(usuario.getRoles()).contains(rolGestorBD);
            verify(usuarioRepository).save(usuario);
            verify(centroRepository).save(centro);
        }

        @Test
        @DisplayName("usuario con rol gestor previo no recibe el rol duplicado")
        void pendiente_usuarioYaTieneRolGestor_noAsignaRolDuplicado() {
            Rol rolGestorExistente = new Rol();
            rolGestorExistente.setId(2L);
            rolGestorExistente.setNombre(RolNombre.GESTOR_CENTRO);
            rolGestorExistente.setUsuarios(new HashSet<>());
            usuario.getRoles().add(rolGestorExistente);

            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));
            when(usuarioRepository.save(any())).thenReturn(usuario);
            when(centroRepository.save(any())).thenReturn(centro);
            when(solicitudGestionRepository.save(any())).thenReturn(solicitudPendiente);
            when(solicitudGestionMapper.toDTO(any())).thenReturn(solicitudDTO);

            solicitudGestionService.aprobarSolicitud(100L);

            verify(rolRepository, never()).findByNombre(any());
            assertThat(usuario.getRoles()).hasSize(1);
        }

        @Test
        @DisplayName("centro no verificado impide la aprobación")
        void centroNoVerificado_lanzaIllegalStateException() {
            centro.setVerificado(false);
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));

            assertThatThrownBy(() -> solicitudGestionService.aprobarSolicitud(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está verificado");

            assertThat(solicitudPendiente.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
            verify(solicitudGestionRepository, never()).save(any());
        }

        @Test
        @DisplayName("solicitud ya procesada lanza IllegalStateException")
        void solicitudYaProcesada_lanzaIllegalStateException() {
            solicitudPendiente.setEstado(EstadoSolicitud.ACEPTADA);
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));

            assertThatThrownBy(() -> solicitudGestionService.aprobarSolicitud(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya ha sido procesada");
        }

        @Test
        @DisplayName("solicitud inexistente lanza IllegalArgumentException")
        void solicitudInexistente_lanzaIllegalArgumentException() {
            when(solicitudGestionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudGestionService.aprobarSolicitud(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("rechazarSolicitud()")
    class RechazarSolicitud {

        @Test
        @DisplayName("solicitud pendiente pasa a estado Rechazada")
        void solicitudPendiente_pasaARechazada() {
            SolicitudGestionDTO rechazadaDTO = new SolicitudGestionDTO();
            rechazadaDTO.setId(100L);
            rechazadaDTO.setEstado(EstadoSolicitud.RECHAZADA);

            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));
            when(solicitudGestionRepository.save(any())).thenReturn(solicitudPendiente);
            when(solicitudGestionMapper.toDTO(any())).thenReturn(rechazadaDTO);

            SolicitudGestionDTO resultado = solicitudGestionService.rechazarSolicitud(100L);

            assertThat(solicitudPendiente.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
            assertThat(solicitudPendiente.getFechaResolucion()).isNotNull();
            assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
            verify(solicitudGestionRepository).save(solicitudPendiente);
        }

        @Test
        @DisplayName("solicitud ya procesada lanza IllegalStateException")
        void solicitudYaProcesada_lanzaIllegalStateException() {
            solicitudPendiente.setEstado(EstadoSolicitud.RECHAZADA);
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));

            assertThatThrownBy(() -> solicitudGestionService.rechazarSolicitud(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya ha sido procesada");
            verify(solicitudGestionRepository, never()).save(any());
        }

        @Test
        @DisplayName("solicitud inexistente lanza IllegalArgumentException")
        void solicitudInexistente_lanzaIllegalArgumentException() {
            when(solicitudGestionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudGestionService.rechazarSolicitud(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("cancelarSolicitud()")
    class CancelarSolicitud {

        @Test
        @DisplayName("usuario dueño con solicitud pendiente cancela correctamente")
        void duenioYPendiente_eliminaSolicitud() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));

            assertThatCode(() -> solicitudGestionService.cancelarSolicitud(100L))
                    .doesNotThrowAnyException();

            verify(solicitudGestionRepository).delete(solicitudPendiente);
        }

        @Test
        @DisplayName("sin roles tras cancelar también elimina el usuario")
        void sinRolesTrasCancel_eliminaUsuario() {
            usuario.setRoles(new ArrayList<>());
            
            // Establece un contexto de seguridad ficticio para validar la autorización del usuario cancelando la solicitud
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));

            solicitudGestionService.cancelarSolicitud(100L);

            verify(solicitudGestionRepository).delete(solicitudPendiente);
            verify(usuarioRepository).delete(usuario);
        }

        @Test
        @DisplayName("usuario no dueño lanza IllegalStateException")
        void usuarioNoDuenio_lanzaIllegalStateException() {
            Usuario otroUsuario = new Usuario();
            otroUsuario.setId(99L);
            otroUsuario.setEmail("otro@test.com");
            otroUsuario.setRoles(new ArrayList<>());
            otroUsuario.setCentrosGestionados(new HashSet<>());

            simularUsuarioAutenticado("otro@test.com");
            when(usuarioRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(otroUsuario));
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));

            assertThatThrownBy(() -> solicitudGestionService.cancelarSolicitud(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("permiso para cancelar");

            verify(solicitudGestionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("solicitud ya procesada lanza IllegalStateException")
        void solicitudYaProcesada_lanzaIllegalStateException() {
            solicitudPendiente.setEstado(EstadoSolicitud.ACEPTADA);
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));

            assertThatThrownBy(() -> solicitudGestionService.cancelarSolicitud(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya ha sido procesada");

            verify(solicitudGestionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("solicitud inexistente lanza IllegalArgumentException")
        void solicitudInexistente_lanzaIllegalArgumentException() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudGestionService.cancelarSolicitud(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solicitud");
        }
    }

    @Nested
    @DisplayName("registrarGestorCentroNuevo()")
    class RegistrarGestorCentroNuevo {

        @Test
        @DisplayName("datos del centro nulos lanzan IllegalArgumentException")
        void datosCentroNuevoNulos_lanzaIllegalArgumentException() throws Exception {
            RegistroGestorRequestDTO dto = new RegistroGestorRequestDTO();
            dto.setDatosCentroNuevo(null);

            MultipartFile archivo = mock(MultipartFile.class);

            assertThatThrownBy(() -> solicitudGestionService.registrarGestorCentroNuevo(dto, archivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("datos del nuevo centro");
        }

        @Test
        @DisplayName("datos completos registra usuario, centro y crea solicitud pendiente")
        void flujoCompleto_registraSatisfactoriamente() throws Exception {
            RegistroGestorRequestDTO dto = new RegistroGestorRequestDTO();
            
            RegistroCentroRequestDTO datosCentroNuevo = new RegistroCentroRequestDTO();
            datosCentroNuevo.setCodigo("28000001");
            
            RegistroRequestDTO datosUsuarioNuevos = new RegistroRequestDTO();
            datosUsuarioNuevos.setEmail("gestor_nuevo@test.com");
            
            dto.setDatosCentroNuevo(datosCentroNuevo);
            dto.setDatosUsuario(datosUsuarioNuevos);

            MultipartFile archivo = mock(MultipartFile.class);
            when(archivo.getBytes()).thenReturn(new byte[]{1, 2, 3});

            CentroDTO centroDTO = new CentroDTO();
            centroDTO.setId(10L);
            UsuarioDTO usuarioDTO = new UsuarioDTO();
            usuarioDTO.setId(1L);

            when(usuarioService.registrarUsuario(datosUsuarioNuevos)).thenReturn(usuarioDTO);
            when(centroService.registrarCentro(datosCentroNuevo)).thenReturn(centroDTO);
            
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(centroRepository.findById(10L)).thenReturn(Optional.of(centro));
            
            when(solicitudGestionRepository.save(any())).thenReturn(solicitudPendiente);

            solicitudGestionService.registrarGestorCentroNuevo(dto, archivo);

            verify(usuarioService).registrarUsuario(datosUsuarioNuevos);
            verify(centroService).registrarCentro(datosCentroNuevo);
            verify(usuarioRepository).findById(1L);
            verify(centroRepository).findById(10L);
            verify(solicitudGestionRepository).save(any(SolicitudGestion.class));
        }
    }

    @Nested
    @DisplayName("registrarGestorCentroExistente()")
    class RegistrarGestorCentroExistente {

        @Test
        @DisplayName("flujo válido crea usuario y guarda solicitud")
        void flujoValido_registraYGuarda() throws Exception {
            RegistroGestorRequestDTO dto = new RegistroGestorRequestDTO();

            dto.setIdCentroExistente(10L);

            RegistroRequestDTO datosUsuarioNuevos = new RegistroRequestDTO();
            datosUsuarioNuevos.setEmail("gestor_existente@test.com");
            dto.setDatosUsuario(datosUsuarioNuevos);

            MultipartFile archivo = mock(MultipartFile.class);
            when(archivo.getBytes()).thenReturn(new byte[]{4, 5, 6});

            UsuarioDTO usuarioDTO = new UsuarioDTO();
            usuarioDTO.setId(1L);

            when(usuarioService.registrarUsuario(datosUsuarioNuevos)).thenReturn(usuarioDTO);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(centroRepository.findById(10L)).thenReturn(Optional.of(centro));

            when(solicitudGestionRepository.save(any())).thenReturn(solicitudPendiente);

            solicitudGestionService.registrarGestorCentroExistente(dto, archivo);

            verify(usuarioService).registrarUsuario(datosUsuarioNuevos);
            verify(usuarioRepository).findById(1L);
            verify(centroRepository).findById(10L);
            verify(solicitudGestionRepository).save(any(SolicitudGestion.class));
        }
    }

    @Nested
    @DisplayName("obtenerSolicitudesPendientesPaginadas()")
    class ObtenerPendientesPaginadas {

        @Test
        @DisplayName("retorna página de solicitudes pendientes")
        void retornaPaginaPendientes() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitudGestion> page = new PageImpl<>(List.of(solicitudPendiente));
            when(solicitudGestionRepository.findByEstado(EstadoSolicitud.PENDIENTE, pageable)).thenReturn(page);
            when(solicitudGestionMapper.toDTO(solicitudPendiente)).thenReturn(solicitudDTO);

            Page<SolicitudGestionDTO> resultado = solicitudGestionService.obtenerSolicitudesPendientesPaginadas(pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
            assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        }
    }

    @Nested
    @DisplayName("obtenerSolicitudesProcesadasPaginadas()")
    class ObtenerProcesadasPaginadas {

        @Test
        @DisplayName("retorna página del historial de solicitudes procesadas")
        void retornaPaginaHistorial() {
            Pageable pageable = PageRequest.of(0, 10);
            SolicitudGestion aprobada = new SolicitudGestion();
            aprobada.setEstado(EstadoSolicitud.ACEPTADA);
            SolicitudGestionDTO aprobadaDTO = new SolicitudGestionDTO();
            aprobadaDTO.setEstado(EstadoSolicitud.ACEPTADA);
            Page<SolicitudGestion> page = new PageImpl<>(List.of(aprobada));

            when(solicitudGestionRepository.obtenerHistorialSolicitudesPaginado(EstadoSolicitud.PENDIENTE, pageable)).thenReturn(page);
            when(solicitudGestionMapper.toDTO(aprobada)).thenReturn(aprobadaDTO);

            Page<SolicitudGestionDTO> resultado = solicitudGestionService.obtenerSolicitudesProcesadasPaginadas(pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
            assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        }
    }

    @Nested
    @DisplayName("buscarPendientesConFiltro()")
    class BuscarPendientesConFiltro {

        @Test
        @DisplayName("filtra con término de búsqueda no vacío")
        void conTerminoBusqueda_normalizaYDelega() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitudGestion> page = new PageImpl<>(List.of(solicitudPendiente));
            when(solicitudGestionRepository.buscarPendientesConFiltro(
                    eq(EstadoSolicitud.PENDIENTE), eq("academia"), isNull(), eq(pageable)))
                    .thenReturn(page);
            when(solicitudGestionMapper.toDTO(solicitudPendiente)).thenReturn(solicitudDTO);

            Page<SolicitudGestionDTO> resultado = solicitudGestionService.buscarPendientesConFiltro("  Academia  ", null, pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("sin término de búsqueda usa cadena vacía")
        void sinTermino_usaCadenaVacia() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitudGestion> page = new PageImpl<>(List.of());
            when(solicitudGestionRepository.buscarPendientesConFiltro(
                    eq(EstadoSolicitud.PENDIENTE), eq(""), eq(true), eq(pageable)))
                    .thenReturn(page);

            Page<SolicitudGestionDTO> resultado = solicitudGestionService.buscarPendientesConFiltro(null, true, pageable);

            assertThat(resultado.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("buscarHistorialConFiltros()")
    class BuscarHistorialConFiltros {

        @Test
        @DisplayName("filtra historial por estado y término de búsqueda")
        void conFiltros_delegaAlRepositorio() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitudGestion> page = new PageImpl<>(List.of(solicitudPendiente));
            when(solicitudGestionRepository.buscarHistorialConFiltros(
                    eq(EstadoSolicitud.PENDIENTE), eq(EstadoSolicitud.ACEPTADA), eq("gestor"), eq(pageable)))
                    .thenReturn(page);
            when(solicitudGestionMapper.toDTO(solicitudPendiente)).thenReturn(solicitudDTO);

            Page<SolicitudGestionDTO> resultado = solicitudGestionService.buscarHistorialConFiltros("Gestor", EstadoSolicitud.ACEPTADA, pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("sin filtros usa cadena vacía y estado null")
        void sinFiltros_usaDefectos() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<SolicitudGestion> page = new PageImpl<>(List.of());
            when(solicitudGestionRepository.buscarHistorialConFiltros(
                    eq(EstadoSolicitud.PENDIENTE), isNull(), eq(""), eq(pageable)))
                    .thenReturn(page);

            Page<SolicitudGestionDTO> resultado = solicitudGestionService.buscarHistorialConFiltros(null, null, pageable);

            assertThat(resultado.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("listarSolicitudesDelUsuario()")
    class ListarSolicitudesDelUsuario {

        @Test
        @DisplayName("retorna todas las solicitudes del usuario autenticado")
        void usuarioAutenticado_retornaListaOrdenada() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findAllByUsuarioOrderByFechaSolicitudDesc(usuario))
                    .thenReturn(List.of(solicitudPendiente));
            when(solicitudGestionMapper.toDTOList(List.of(solicitudPendiente))).thenReturn(List.of(solicitudDTO));

            List<SolicitudGestionDTO> resultado = solicitudGestionService.listarSolicitudesDelUsuario();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("obtenerSolicitudDelUsuarioPorId()")
    class ObtenerSolicitudDelUsuarioPorId {

        @Test
        @DisplayName("solicitud propia existente retorna DTO")
        void solicitudPropia_retornaDTO() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findByIdAndUsuario(100L, usuario))
                    .thenReturn(Optional.of(solicitudPendiente));
            when(solicitudGestionMapper.toDTO(solicitudPendiente)).thenReturn(solicitudDTO);

            SolicitudGestionDTO resultado = solicitudGestionService.obtenerSolicitudDelUsuarioPorId(100L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("solicitud ajena o inexistente lanza IllegalArgumentException")
        void solicitudAjenaOInexistente_lanzaIllegalArgumentException() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findByIdAndUsuario(999L, usuario)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudGestionService.obtenerSolicitudDelUsuarioPorId(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("permiso");
        }
    }

    @Nested
    @DisplayName("enviarSolicitudUsuarioAutenticado()")
    class EnviarSolicitudUsuarioAutenticado {

        @Test
        @DisplayName("flujo correcto guarda la solicitud con los bytes del archivo")
        void flujoValido_guardaSolicitud() throws Exception {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(centroRepository.findById(10L)).thenReturn(Optional.of(centro));
            when(solicitudGestionRepository.existsByUsuarioIdAndCentroIdAndEstado(
                    1L, 10L, EstadoSolicitud.PENDIENTE)).thenReturn(false);

            MultipartFile prueba = mock(MultipartFile.class);
            when(prueba.isEmpty()).thenReturn(false);
            when(prueba.getBytes()).thenReturn(new byte[]{9, 8, 7});
            when(solicitudGestionRepository.save(any())).thenReturn(solicitudPendiente);
            when(solicitudGestionMapper.toDTO(solicitudPendiente)).thenReturn(solicitudDTO);

            SolicitudGestionDTO resultado = solicitudGestionService.enviarSolicitudUsuarioAutenticado(10L, prueba);

            assertThat(resultado).isNotNull();
            verify(solicitudGestionRepository).save(any(SolicitudGestion.class));
        }

        @Test
        @DisplayName("solicitud duplicada pendiente lanza IllegalStateException")
        void solicitudDuplicada_lanzaIllegalStateException() throws Exception {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(centroRepository.findById(10L)).thenReturn(Optional.of(centro));
            when(solicitudGestionRepository.existsByUsuarioIdAndCentroIdAndEstado(
                    1L, 10L, EstadoSolicitud.PENDIENTE)).thenReturn(true);

            MultipartFile prueba = mock(MultipartFile.class);

            assertThatThrownBy(() -> solicitudGestionService.enviarSolicitudUsuarioAutenticado(10L, prueba))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solicitud pendiente");
            verify(solicitudGestionRepository, never()).save(any());
        }

        @Test
        @DisplayName("prueba nula lanza IllegalArgumentException")
        void pruebaNula_lanzaIllegalArgumentException() throws Exception {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(centroRepository.findById(10L)).thenReturn(Optional.of(centro));
            when(solicitudGestionRepository.existsByUsuarioIdAndCentroIdAndEstado(
                    1L, 10L, EstadoSolicitud.PENDIENTE)).thenReturn(false);

            assertThatThrownBy(() -> solicitudGestionService.enviarSolicitudUsuarioAutenticado(10L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obligatorio");
        }

        @Test
        @DisplayName("centro inexistente lanza IllegalArgumentException")
        void centroInexistente_lanzaIllegalArgumentException() throws Exception {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(centroRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudGestionService.enviarSolicitudUsuarioAutenticado(999L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("identificador");
        }
    }

    @Nested
    @DisplayName("enviarSolicitudConCentroNuevo()")
    class EnviarSolicitudConCentroNuevo {

        @Test
        @DisplayName("prueba nula lanza IllegalArgumentException antes de registrar el centro")
        void pruebaNula_lanzaAntesDeRegistrar() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));

            RegistroCentroRequestDTO datosCentro = new RegistroCentroRequestDTO();

            assertThatThrownBy(() -> solicitudGestionService.enviarSolicitudConCentroNuevo(datosCentro, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obligatorio");
            verifyNoInteractions(centroService);
        }

        @Test
        @DisplayName("flujo correcto registra centro, recupera entidad y guarda solicitud")
        void flujoValido_registraCentroYGuardaSolicitud() throws Exception {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));

            RegistroCentroRequestDTO datosCentro = new RegistroCentroRequestDTO();
            CentroDTO centroDTO = new CentroDTO();
            centroDTO.setId(10L);

            when(centroService.registrarCentro(datosCentro)).thenReturn(centroDTO);
            when(centroRepository.findById(10L)).thenReturn(Optional.of(centro));

            MultipartFile prueba = mock(MultipartFile.class);
            when(prueba.isEmpty()).thenReturn(false);
            when(prueba.getBytes()).thenReturn(new byte[]{1, 2, 3});
            when(solicitudGestionRepository.save(any())).thenReturn(solicitudPendiente);
            when(solicitudGestionMapper.toDTO(solicitudPendiente)).thenReturn(solicitudDTO);

            SolicitudGestionDTO resultado = solicitudGestionService.enviarSolicitudConCentroNuevo(datosCentro, prueba);

            assertThat(resultado).isNotNull();
            verify(centroService).registrarCentro(datosCentro);
            verify(solicitudGestionRepository).save(any(SolicitudGestion.class));
        }
    }

    @Nested
    @DisplayName("obtenerCentroDeSolicitud()")
    class ObtenerCentroDeSolicitud {

        @Test
        @DisplayName("solicitud existente retorna el DTO del centro vinculado")
        void solicitudExistente_retornaCentroDTO() {
            CentroDTO centroDTO = new CentroDTO();
            centroDTO.setId(10L);
            when(solicitudGestionRepository.findById(100L)).thenReturn(Optional.of(solicitudPendiente));
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            CentroDTO resultado = solicitudGestionService.obtenerCentroDeSolicitud(100L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("solicitud inexistente lanza IllegalArgumentException")
        void solicitudInexistente_lanzaIllegalArgumentException() {
            when(solicitudGestionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudGestionService.obtenerCentroDeSolicitud(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("obtenerCentroDePropiaSolicitud()")
    class ObtenerCentroDePropiaSolicitud {

        @Test
        @DisplayName("solicitud propia existente retorna el DTO del centro")
        void solicitudPropia_retornaCentroDTO() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findByIdAndUsuario(100L, usuario))
                    .thenReturn(Optional.of(solicitudPendiente));
            CentroDTO centroDTO = new CentroDTO();
            centroDTO.setId(10L);
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            CentroDTO resultado = solicitudGestionService.obtenerCentroDePropiaSolicitud(100L);

            assertThat(resultado.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("solicitud ajena lanza IllegalArgumentException")
        void solicitudAjena_lanzaIllegalArgumentException() {
            simularUsuarioAutenticado("gestor@test.com");
            when(usuarioRepository.findByEmail("gestor@test.com")).thenReturn(Optional.of(usuario));
            when(solicitudGestionRepository.findByIdAndUsuario(100L, usuario)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudGestionService.obtenerCentroDePropiaSolicitud(100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("permiso");
        }
    }
}
