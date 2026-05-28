package com.irisperez.tfg.encuentraformacion.controller.admin;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.NuevaSolicitudGestionDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.gestion.SolicitudGestionDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.service.solicitud.gestion.SolicitudGestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitudesGestionController")
class SolicitudesGestionControllerTest {

    @Mock private SolicitudGestionService solicitudGestionService;

    @InjectMocks
    private SolicitudesGestionController solicitudesGestionController;

    private SolicitudGestionDTO solicitudPendienteDTO;
    private SolicitudGestionDTO solicitudAceptadaDTO;

    @BeforeEach
    void setUp() {
        solicitudPendienteDTO = new SolicitudGestionDTO();
        solicitudPendienteDTO.setId(1L);
        solicitudPendienteDTO.setIdUsuario(10L);
        solicitudPendienteDTO.setNombre("Ana");
        solicitudPendienteDTO.setApellidos("García");
        solicitudPendienteDTO.setIdCentro(20L);
        solicitudPendienteDTO.setNombreCentro("Academia TestFP");
        solicitudPendienteDTO.setVerificadoCentro(true);
        solicitudPendienteDTO.setEstado(EstadoSolicitud.PENDIENTE);
        solicitudPendienteDTO.setFechaSolicitud(LocalDateTime.now().minusDays(2));

        solicitudAceptadaDTO = new SolicitudGestionDTO();
        solicitudAceptadaDTO.setId(2L);
        solicitudAceptadaDTO.setEstado(EstadoSolicitud.ACEPTADA);
        solicitudAceptadaDTO.setFechaSolicitud(LocalDateTime.now().minusDays(5));
        solicitudAceptadaDTO.setFechaResolucion(LocalDateTime.now().minusDays(1));
    }

    @Nested
    @DisplayName("obtenerSolicitudesProcesadas()")
    class ObtenerProcesadas {

        @Test
        @DisplayName("historial existente retorna 200 con la página")
        void retornaHistorial_con200() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitudGestionDTO> page = new PageImpl<>(List.of(solicitudAceptadaDTO), pageable, 1);
            when(solicitudGestionService.buscarHistorialConFiltros(any(), any(), any(Pageable.class))).thenReturn(page);

            ResponseEntity<Page<SolicitudGestionDTO>> response = solicitudesGestionController.obtenerSolicitudesProcesadas(pageable, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("sin procesadas retorna 200 con página vacía")
        void sinHistorial_retorna200PaginaVacia() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitudGestionDTO> page = new PageImpl<>(List.of(), pageable, 0);
            when(solicitudGestionService.buscarHistorialConFiltros(any(), any(), any(Pageable.class))).thenReturn(page);

            ResponseEntity<Page<SolicitudGestionDTO>> response = solicitudesGestionController.obtenerSolicitudesProcesadas(pageable, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerSolicitudesPendientes()")
    class ObtenerPendientes {

        @Test
        @DisplayName("pendientes existentes retorna 200 con la página")
        void retornaPendientes_con200() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitudGestionDTO> page = new PageImpl<>(List.of(solicitudPendienteDTO), pageable, 1);
            when(solicitudGestionService.buscarPendientesConFiltro(any(), any(), any(Pageable.class))).thenReturn(page);

            ResponseEntity<Page<SolicitudGestionDTO>> response = solicitudesGestionController.obtenerSolicitudesPendientes(pageable, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("ninguna pendiente retorna 200 con página vacía")
        void ninguna_retorna200PaginaVacia() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitudGestionDTO> page = new PageImpl<>(List.of(), pageable, 0);
            when(solicitudGestionService.buscarPendientesConFiltro(any(), any(), any(Pageable.class))).thenReturn(page);

            ResponseEntity<Page<SolicitudGestionDTO>> response = solicitudesGestionController.obtenerSolicitudesPendientes(pageable, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerMiSolicitud()")
    class ObtenerMiSolicitud {

        @Test
        @DisplayName("usuario con solicitud retorna 200")
        void conSolicitud_retorna200() {
            when(solicitudGestionService.obtenerSolicitudDelUsuario())
                    .thenReturn(solicitudPendienteDTO);

            ResponseEntity<?> response = solicitudesGestionController.obtenerMiSolicitud();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(solicitudPendienteDTO);
        }

        @Test
        @DisplayName("sin solicitud propaga IllegalStateException")
        void sinSolicitud_propagaIllegalStateException() {
            when(solicitudGestionService.obtenerSolicitudDelUsuario())
                    .thenThrow(new IllegalStateException("No tienes ninguna solicitud pendiente de revisión."));

            assertThatThrownBy(() -> solicitudesGestionController.obtenerMiSolicitud())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No tienes ninguna solicitud pendiente");
        }
    }

    @Nested
    @DisplayName("obtenerMiCentro()")
    class ObtenerMiCentro {

        @Test
        @DisplayName("usuario con solicitud retorna 200 con el centro")
        void conSolicitud_retorna200ConCentro() {
            CentroDTO centroDTO = new CentroDTO();
            centroDTO.setId(20L);
            centroDTO.setCodigo("28000001");
            centroDTO.setTipo(TipoCentro.PRIVADO);
            centroDTO.setVerificado(true);

            when(solicitudGestionService.obtenerSolicitudDelUsuario()).thenReturn(solicitudPendienteDTO);
            when(solicitudGestionService.obtenerCentroDeSolicitud(1L)).thenReturn(centroDTO);

            ResponseEntity<?> response = solicitudesGestionController.obtenerMiCentro();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(centroDTO);
        }
    }

    @Nested
    @DisplayName("obtenerPruebaTitularidad()")
    class ObtenerImagen {

        @Test
        @DisplayName("bytes PNG retorna 200 con Content-Type image/png")
        void imagenPng_retorna200ConContentTypePng() {
            byte[] pngBytes = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            when(solicitudGestionService.obtenerPruebaTitularidad(1L)).thenReturn(pngBytes);

            ResponseEntity<ByteArrayResource> response =
                    solicitudesGestionController.obtenerPruebaTitularidad(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("Content-Type")).contains("image/png");
        }

        @Test
        @DisplayName("bytes JPEG retorna 200 con Content-Type image/jpeg")
        void imagenJpeg_retorna200ConContentTypeJpeg() {
            byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
            when(solicitudGestionService.obtenerPruebaTitularidad(1L)).thenReturn(jpegBytes);

            ResponseEntity<ByteArrayResource> response =
                    solicitudesGestionController.obtenerPruebaTitularidad(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("Content-Type")).contains("image/jpeg");
        }

        @Test
        @DisplayName("id inexistente propaga IllegalArgumentException")
        void idInexistente_propagaIllegalArgumentException() {
            when(solicitudGestionService.obtenerPruebaTitularidad(999L))
                    .thenThrow(new IllegalArgumentException("No se ha encontrado la solicitud con id: 999"));

            assertThatThrownBy(() -> solicitudesGestionController.obtenerPruebaTitularidad(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("aprobarSolicitud()")
    class AprobarSolicitud {

        @Test
        @DisplayName("solicitud pendiente retorna 200 aceptada")
        void pendiente_retorna200Aceptada() {
            solicitudPendienteDTO.setEstado(EstadoSolicitud.ACEPTADA);
            when(solicitudGestionService.aprobarSolicitud(1L)).thenReturn(solicitudPendienteDTO);

            ResponseEntity<?> response = solicitudesGestionController.aprobarSolicitud(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(solicitudPendienteDTO);
        }

        @Test
        @DisplayName("centro no verificado propaga IllegalStateException")
        void centroNoVerificado_propagaIllegalStateException() {
            when(solicitudGestionService.aprobarSolicitud(1L))
                    .thenThrow(new IllegalStateException("No se puede aprobar la solicitud, el centro no está verificado."));

            assertThatThrownBy(() -> solicitudesGestionController.aprobarSolicitud(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está verificado");
        }

        @Test
        @DisplayName("solicitud ya procesada propaga IllegalStateException")
        void yaProcessada_propagaIllegalStateException() {
            when(solicitudGestionService.aprobarSolicitud(1L))
                    .thenThrow(new IllegalStateException("La solicitud ya ha sido procesada anteriormente."));

            assertThatThrownBy(() -> solicitudesGestionController.aprobarSolicitud(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya ha sido procesada");
        }
    }

    @Nested
    @DisplayName("rechazarSolicitud()")
    class RechazarSolicitud {

        @Test
        @DisplayName("solicitud pendiente retorna 200 rechazada")
        void pendiente_retorna200Rechazada() {
            solicitudPendienteDTO.setEstado(EstadoSolicitud.RECHAZADA);
            when(solicitudGestionService.rechazarSolicitud(1L)).thenReturn(solicitudPendienteDTO);

            ResponseEntity<?> response = solicitudesGestionController.rechazarSolicitud(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(solicitudPendienteDTO);
        }

        @Test
        @DisplayName("solicitud ya procesada propaga IllegalStateException")
        void yaProcessada_propagaIllegalStateException() {
            when(solicitudGestionService.rechazarSolicitud(1L))
                    .thenThrow(new IllegalStateException("La solicitud ya ha sido procesada anteriormente."));

            assertThatThrownBy(() -> solicitudesGestionController.rechazarSolicitud(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya ha sido procesada");
        }
    }

    @Nested
    @DisplayName("cancelarSolicitud()")
    class CancelarSolicitud {

        @Test
        @DisplayName("usuario dueño con solicitud pendiente retorna 200 con mensaje")
        void duenioPendiente_retorna200ConMensaje() {
            doNothing().when(solicitudGestionService).cancelarSolicitud(1L);

            ResponseEntity<?> response = solicitudesGestionController.cancelarSolicitud(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertThat(body).containsEntry("mensaje", "Solicitud cancelada correctamente.");
            verify(solicitudGestionService).cancelarSolicitud(1L);
        }

        @Test
        @DisplayName("usuario no dueño propaga IllegalStateException")
        void noDuenio_propagaIllegalStateException() {
            doThrow(new IllegalStateException("No tienes permiso para cancelar esta solicitud."))
                    .when(solicitudGestionService).cancelarSolicitud(1L);

            assertThatThrownBy(() -> solicitudesGestionController.cancelarSolicitud(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No tienes permiso");
        }

        @Test
        @DisplayName("solicitud ya procesada propaga IllegalStateException")
        void solicitudYaProcesada_propagaIllegalStateException() {
            doThrow(new IllegalStateException("No se puede cancelar la solicitud porque ya ha sido procesada."))
                    .when(solicitudGestionService).cancelarSolicitud(1L);

            assertThatThrownBy(() -> solicitudesGestionController.cancelarSolicitud(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya ha sido procesada");
        }
    }

    @Nested
    @DisplayName("listarMisSolicitudes()")
    class ListarMisSolicitudes {

        @Test
        @DisplayName("usuario con solicitudes retorna 200 con la lista")
        void conSolicitudes_retorna200ConLista() {
            when(solicitudGestionService.listarSolicitudesDelUsuario())
                    .thenReturn(List.of(solicitudPendienteDTO, solicitudAceptadaDTO));

            ResponseEntity<List<SolicitudGestionDTO>> response =
                    solicitudesGestionController.listarMisSolicitudes();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("usuario sin solicitudes retorna 200 con lista vacía")
        void sinSolicitudes_retorna200ListaVacia() {
            when(solicitudGestionService.listarSolicitudesDelUsuario()).thenReturn(List.of());

            ResponseEntity<List<SolicitudGestionDTO>> response =
                    solicitudesGestionController.listarMisSolicitudes();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("enviarSolicitud()")
    class EnviarSolicitud {

        @Test
        @DisplayName("solicitud válida retorna 201 con el DTO creado")
        void solicitudValida_retorna201() throws Exception {
            NuevaSolicitudGestionDTO datos = new NuevaSolicitudGestionDTO();
            datos.setCentroId(20L);
            MockMultipartFile prueba = new MockMultipartFile(
                    "pruebaTitularidad", "doc.jpg", "image/jpeg", new byte[]{1, 2, 3});

            when(solicitudGestionService.enviarSolicitudUsuarioAutenticado(20L, prueba))
                    .thenReturn(solicitudPendienteDTO);

            ResponseEntity<SolicitudGestionDTO> response =
                    solicitudesGestionController.enviarSolicitud(datos, prueba);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(solicitudPendienteDTO);
        }

        @Test
        @DisplayName("centro inexistente propaga IllegalArgumentException")
        void centroInexistente_propagaIllegalArgumentException() throws Exception {
            NuevaSolicitudGestionDTO datos = new NuevaSolicitudGestionDTO();
            datos.setCentroId(999L);
            MockMultipartFile prueba = new MockMultipartFile(
                    "pruebaTitularidad", "doc.jpg", "image/jpeg", new byte[]{1, 2, 3});

            when(solicitudGestionService.enviarSolicitudUsuarioAutenticado(999L, prueba))
                    .thenThrow(new IllegalArgumentException("No existe ningún centro con ese identificador."));

            assertThatThrownBy(() -> solicitudesGestionController.enviarSolicitud(datos, prueba))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe ningún centro");
        }

        @Test
        @DisplayName("solicitud duplicada para el mismo centro propaga IllegalStateException")
        void duplicado_propagaIllegalStateException() throws Exception {
            NuevaSolicitudGestionDTO datos = new NuevaSolicitudGestionDTO();
            datos.setCentroId(20L);
            MockMultipartFile prueba = new MockMultipartFile(
                    "pruebaTitularidad", "doc.jpg", "image/jpeg", new byte[]{1, 2, 3});

            when(solicitudGestionService.enviarSolicitudUsuarioAutenticado(20L, prueba))
                    .thenThrow(new IllegalStateException("Ya tienes una solicitud pendiente para ese centro."));

            assertThatThrownBy(() -> solicitudesGestionController.enviarSolicitud(datos, prueba))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solicitud pendiente para ese centro");
        }

        @Test
        @DisplayName("sin prueba de titularidad propaga IllegalArgumentException")
        void sinPrueba_propagaIllegalArgumentException() throws Exception {
            NuevaSolicitudGestionDTO datos = new NuevaSolicitudGestionDTO();
            datos.setCentroId(20L);
            MockMultipartFile prueba = new MockMultipartFile(
                    "pruebaTitularidad", "doc.jpg", "image/jpeg", new byte[0]);

            when(solicitudGestionService.enviarSolicitudUsuarioAutenticado(20L, prueba))
                    .thenThrow(new IllegalArgumentException("El documento que prueba tu vinculación con el centro es obligatorio."));

            assertThatThrownBy(() -> solicitudesGestionController.enviarSolicitud(datos, prueba))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vinculación con el centro");
        }
    }

    @Nested
    @DisplayName("enviarSolicitudConCentroNuevo()")
    class EnviarSolicitudConCentroNuevo {

        @Test
        @DisplayName("datos válidos retorna 201 con el DTO creado")
        void datosValidos_retorna201() throws Exception {
            RegistroCentroRequestDTO datosCentro = new RegistroCentroRequestDTO();
            datosCentro.setCodigo("28000099");
            datosCentro.setNombreComercial("Centro Nuevo");
            MockMultipartFile prueba = new MockMultipartFile(
                    "pruebaTitularidad", "doc.jpg", "image/jpeg", new byte[]{1, 2, 3});

            when(solicitudGestionService.enviarSolicitudConCentroNuevo(datosCentro, prueba))
                    .thenReturn(solicitudPendienteDTO);

            ResponseEntity<SolicitudGestionDTO> response =
                    solicitudesGestionController.enviarSolicitudConCentroNuevo(datosCentro, prueba);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(solicitudPendienteDTO);
        }

        @Test
        @DisplayName("código de centro duplicado propaga IllegalArgumentException")
        void codigoDuplicado_propagaIllegalArgumentException() throws Exception {
            RegistroCentroRequestDTO datosCentro = new RegistroCentroRequestDTO();
            datosCentro.setCodigo("28000001");
            MockMultipartFile prueba = new MockMultipartFile(
                    "pruebaTitularidad", "doc.jpg", "image/jpeg", new byte[]{1, 2, 3});

            when(solicitudGestionService.enviarSolicitudConCentroNuevo(datosCentro, prueba))
                    .thenThrow(new IllegalArgumentException("El código de centro ya está registrado."));

            assertThatThrownBy(() ->
                    solicitudesGestionController.enviarSolicitudConCentroNuevo(datosCentro, prueba))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya está registrado");
        }

        @Test
        @DisplayName("sin prueba de titularidad propaga IllegalArgumentException")
        void sinPrueba_propagaIllegalArgumentException() throws Exception {
            RegistroCentroRequestDTO datosCentro = new RegistroCentroRequestDTO();
            datosCentro.setCodigo("28000099");
            MockMultipartFile prueba = new MockMultipartFile(
                    "pruebaTitularidad", "doc.jpg", "image/jpeg", new byte[0]);

            when(solicitudGestionService.enviarSolicitudConCentroNuevo(datosCentro, prueba))
                    .thenThrow(new IllegalArgumentException("El documento que prueba tu vinculación con el centro es obligatorio."));

            assertThatThrownBy(() ->
                    solicitudesGestionController.enviarSolicitudConCentroNuevo(datosCentro, prueba))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vinculación con el centro");
        }
    }

    @Nested
    @DisplayName("obtenerMiSolicitudPorId()")
    class ObtenerMiSolicitudPorId {

        @Test
        @DisplayName("solicitud propia retorna 200")
        void solicitudPropia_retorna200() {
            when(solicitudGestionService.obtenerSolicitudDelUsuarioPorId(1L))
                    .thenReturn(solicitudPendienteDTO);

            ResponseEntity<SolicitudGestionDTO> response =
                    solicitudesGestionController.obtenerMiSolicitudPorId(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(solicitudPendienteDTO);
        }

        @Test
        @DisplayName("solicitud ajena propaga IllegalArgumentException")
        void solicitudAjena_propagaIllegalArgumentException() {
            when(solicitudGestionService.obtenerSolicitudDelUsuarioPorId(99L))
                    .thenThrow(new IllegalArgumentException("No se ha encontrado la solicitud o no tienes permiso para verla."));

            assertThatThrownBy(() -> solicitudesGestionController.obtenerMiSolicitudPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("permiso para verla");
        }
    }
}
