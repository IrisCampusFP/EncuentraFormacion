package com.irisperez.tfg.encuentraformacion.controller.admin;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.service.centro.CentroService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CentrosController")
class CentrosControllerTest {

    @Mock private CentroService centroService;

    @InjectMocks
    private CentrosController centrosController;

    private CentroDTO centroDTO;
    private RegistroCentroRequestDTO registroDTO;

    @BeforeEach
    void setUp() {
        centroDTO = new CentroDTO();
        centroDTO.setId(1L);
        centroDTO.setNombreComercial("Academia TestFP");
        centroDTO.setCodigo("28000001");
        centroDTO.setDireccion("Calle Mayor 1");
        centroDTO.setLocalidad("Madrid");
        centroDTO.setProvincia("Madrid");
        centroDTO.setTipo(TipoCentro.PRIVADO);
        centroDTO.setVerificado(false);
        centroDTO.setTieneGestor(false);
        centroDTO.setFechaAlta(LocalDateTime.now());

        registroDTO = new RegistroCentroRequestDTO();
        registroDTO.setNombreComercial("Academia TestFP");
        registroDTO.setCodigo("28000001");
        registroDTO.setDireccion("Calle Mayor 1");
        registroDTO.setLocalidad("Madrid");
        registroDTO.setProvincia("Madrid");
        registroDTO.setTipo(TipoCentro.PRIVADO);
    }

    @Nested
    @DisplayName("obtenerCentros()")
    class ObtenerCentros {

        @Test
        @DisplayName("lista de centros retorna 200 con la lista completa")
        void retornaListaCompleta_con200() {
            when(centroService.obtenerCentros()).thenReturn(List.of(centroDTO));

            ResponseEntity<?> response = centrosController.obtenerCentros();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(List.of(centroDTO));
        }

        @Test
        @DisplayName("lista vacía retorna 200 con array vacío")
        void listaVacia_retorna200ArrayVacio() {
            when(centroService.obtenerCentros()).thenReturn(List.of());

            ResponseEntity<?> response = centrosController.obtenerCentros();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat((List<?>) response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerCentrosVerificados()")
    class ObtenerCentrosVerificados {

        @Test
        @DisplayName("delega en el servicio y retorna 200 con centros verificados")
        void retornaSoloVerificados_con200() {
            centroDTO.setVerificado(true);
            Pageable pageable = PageRequest.of(0, 10);
            Page<CentroDTO> page = new PageImpl<>(List.of(centroDTO), pageable, 1);
            when(centroService.buscarVerificadosConFiltros(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

            ResponseEntity<Page<CentroDTO>> response = centrosController.obtenerCentrosVerificados(pageable, null, null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(centroService).buscarVerificadosConFiltros(any(), any(), any(), any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("obtenerCentrosNoVerificados()")
    class ObtenerCentrosSinVerificar {

        @Test
        @DisplayName("delega en el servicio y retorna 200 con centros no verificados")
        void retornaSoloNoVerificados_con200() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<CentroDTO> page = new PageImpl<>(List.of(centroDTO), pageable, 1);
            when(centroService.buscarSinVerificarConFiltros(any(), any(), any(), any(Pageable.class))).thenReturn(page);

            ResponseEntity<Page<CentroDTO>> response = centrosController.obtenerCentrosNoVerificados(pageable, null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(centroService).buscarSinVerificarConFiltros(any(), any(), any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("obtenerCentroPorId()")
    class ObtenerCentroPorId {

        @Test
        @DisplayName("id existente retorna 200 con el centro")
        void idExistente_retorna200ConCentro() {
            when(centroService.obtenerCentroDTOPorId(1L)).thenReturn(centroDTO);

            ResponseEntity<?> response = centrosController.obtenerCentroPorId(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(centroDTO);
        }

        @Test
        @DisplayName("id inexistente propaga IllegalArgumentException")
        void idInexistente_propagaIllegalArgumentException() {
            when(centroService.obtenerCentroDTOPorId(999L))
                    .thenThrow(new IllegalArgumentException("No se ha encontrado ningún centro con id: 999"));

            assertThatThrownBy(() -> centrosController.obtenerCentroPorId(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("comprobarCentroPorCodigo()")
    class ComprobarCentroPorCodigo {

        @Test
        @DisplayName("código existente retorna 200 con datos del centro")
        void codigoExistente_retorna200ConCentro() {
            when(centroService.existeCodigo("28000001")).thenReturn(true);
            when(centroService.obtenerCentroDTOPorCodigo("28000001")).thenReturn(centroDTO);

            ResponseEntity<?> response = centrosController.comprobarCentroPorCodigo("28000001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(centroDTO);
        }

        @Test
        @DisplayName("código no registrado retorna 404")
        void codigoNoExistente_retorna404() {
            when(centroService.existeCodigo("99000001")).thenReturn(false);

            ResponseEntity<?> response = centrosController.comprobarCentroPorCodigo("99000001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("crearCentro()")
    class CrearCentro {

        @Test
        @DisplayName("DTO válido retorna 201 y verifica el centro")
        void dtoValido_retorna201Verificado() {
            CentroDTO centroVerificado = new CentroDTO();
            centroVerificado.setId(1L);
            centroVerificado.setCodigo("28000001");
            centroVerificado.setVerificado(true);

            when(centroService.registrarCentro(any())).thenReturn(centroDTO);
            doNothing().when(centroService).verificarCentro(1L);
            when(centroService.obtenerCentroDTOPorId(1L)).thenReturn(centroVerificado);

            ResponseEntity<?> response = centrosController.crearCentro(registroDTO);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(centroService).verificarCentro(1L);
        }

        @Test
        @DisplayName("código duplicado propaga IllegalArgumentException")
        void codigoDuplicado_propagaIllegalArgumentException() {
            when(centroService.registrarCentro(any()))
                    .thenThrow(new IllegalArgumentException("Ya existe un centro con ese código."));

            assertThatThrownBy(() -> centrosController.crearCentro(registroDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un centro con ese código.");
        }
    }

    @Nested
    @DisplayName("editarCentro()")
    class EditarCentro {

        @Test
        @DisplayName("datos válidos retorna 200 con centro actualizado")
        void datosValidos_retorna200ConCentroActualizado() {
            CentroUpdateDTO updateDTO = new CentroUpdateDTO();
            updateDTO.setCodigo("28000001");
            updateDTO.setNombreComercial("Nueva Academia FP");

            centroDTO.setNombreComercial("Nueva Academia FP");
            when(centroService.actualizarCentro(eq(1L), any())).thenReturn(centroDTO);

            ResponseEntity<?> response = centrosController.editarCentro(1L, updateDTO);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(centroDTO);
        }
    }

    @Nested
    @DisplayName("eliminarCentro()")
    class EliminarCentro {

        @Test
        @DisplayName("id existente retorna 200 e invoca el borrado")
        void idExistente_retorna200() {
            doNothing().when(centroService).eliminarCentro(1L);

            ResponseEntity<?> response = centrosController.eliminarCentro(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(centroService).eliminarCentro(1L);
        }

        @Test
        @DisplayName("id inexistente propaga IllegalStateException")
        void idInexistente_propagaIllegalStateException() {
            doThrow(new IllegalStateException("No se ha encontrado ningún centro con id: 99"))
                    .when(centroService).eliminarCentro(99L);

            assertThatThrownBy(() -> centrosController.eliminarCentro(99L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("verificarCentro() / quitarVerificacionCentro()")
    class Verificacion {

        @Test
        @DisplayName("verificarCentro retorna 200 e invoca el servicio")
        void verificar_retorna200() {
            doNothing().when(centroService).verificarCentro(1L);

            ResponseEntity<?> response = centrosController.verificarCentro(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(centroService).verificarCentro(1L);
        }

        @Test
        @DisplayName("quitarVerificacionCentro retorna 200 e invoca el servicio")
        void quitarVerificacion_retorna200() {
            doNothing().when(centroService).quitarVerificacionCentro(1L);

            ResponseEntity<?> response = centrosController.quitarVerificacionCentro(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(centroService).quitarVerificacionCentro(1L);
        }
    }
}
