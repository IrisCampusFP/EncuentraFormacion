package com.irisperez.tfg.encuentraformacion.controller.estudiante;

import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.CrearSolicitudDTO;
import com.irisperez.tfg.encuentraformacion.dto.solicitud.formacion.SolicitudResumenDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.entity.Rol;
import com.irisperez.tfg.encuentraformacion.model.enums.RolNombre;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.security.CustomUserDetails;
import com.irisperez.tfg.encuentraformacion.service.solicitud.formacion.SolicitudService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitudFormacionController")
class SolicitudFormacionControllerTest {

    @Mock private SolicitudService solicitudService;
    @InjectMocks private SolicitudFormacionController controller;

    private CustomUserDetails userDetails;
    private UUID formacionUuid;

    @BeforeEach
    void setUp() {
        Usuario u = new Usuario(); u.setId(1L);
        Rol rol = new Rol(); rol.setNombre(RolNombre.ESTUDIANTE);
        u.setRoles(List.of(rol));
        userDetails = new CustomUserDetails(u);
        formacionUuid = UUID.randomUUID();
    }

    @Nested
    @DisplayName("getMisSolicitudes()")
    class GetTests {
        @Test
        @DisplayName("devuelve página de solicitudes")
        void getMisSolicitudes_ok() {
            Page<SolicitudResumenDTO> page = new PageImpl<>(List.of(new SolicitudResumenDTO()));
            when(solicitudService.getMisSolicitudes(eq(1L), any(), any())).thenReturn(page);

            ResponseEntity<Page<SolicitudResumenDTO>> resp =
                controller.getMisSolicitudes(null, PageRequest.of(0, 10), userDetails);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("filtra por estado")
        void getMisSolicitudes_conEstado() {
            Page<SolicitudResumenDTO> page = new PageImpl<>(List.of());
            when(solicitudService.getMisSolicitudes(eq(1L), eq(EstadoSolicitud.PENDIENTE), any())).thenReturn(page);

            ResponseEntity<Page<SolicitudResumenDTO>> resp =
                controller.getMisSolicitudes(EstadoSolicitud.PENDIENTE, PageRequest.of(0, 10), userDetails);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("check()")
    class CheckTests {
        @Test
        @DisplayName("200 con solicitud existente")
        void check_conSolicitud() {
            SolicitudResumenDTO dto = new SolicitudResumenDTO();
            when(solicitudService.checkByFormacionUuid(formacionUuid, 1L)).thenReturn(Optional.of(dto));

            ResponseEntity<SolicitudResumenDTO> resp = controller.check(formacionUuid, userDetails);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isEqualTo(dto);
        }

        @Test
        @DisplayName("404 si no hay solicitud para esa formación")
        void check_sinSolicitud() {
            when(solicitudService.checkByFormacionUuid(formacionUuid, 1L)).thenReturn(Optional.empty());

            ResponseEntity<SolicitudResumenDTO> resp = controller.check(formacionUuid, userDetails);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("crear()")
    class CrearTests {
        @Test
        @DisplayName("crea solicitud y devuelve 201")
        void crear_ok() {
            CrearSolicitudDTO dto = new CrearSolicitudDTO(); dto.setFormacionUuid(formacionUuid);
            SolicitudResumenDTO resultado = new SolicitudResumenDTO();
            when(solicitudService.crear(dto, 1L)).thenReturn(resultado);

            ResponseEntity<SolicitudResumenDTO> resp = controller.crear(dto, userDetails);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resp.getBody()).isEqualTo(resultado);
        }
    }

    @Nested
    @DisplayName("cancelar()")
    class CancelarTests {
        @Test
        @DisplayName("cancela y devuelve 204")
        void cancelar_ok() {
            doNothing().when(solicitudService).cancelar(3L, 1L);

            ResponseEntity<Void> resp = controller.cancelar(3L, userDetails);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
