package com.irisperez.tfg.encuentraformacion.service.centro;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroDTO;
import java.util.HashSet;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroUpdateDTO;
import com.irisperez.tfg.encuentraformacion.dto.auth.RegistroCentroRequestDTO;
import com.irisperez.tfg.encuentraformacion.dto.usuario.UsuarioDTO;
import com.irisperez.tfg.encuentraformacion.mapper.centro.CentroMapper;
import com.irisperez.tfg.encuentraformacion.mapper.usuario.UsuarioMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.service.catalogo.ProvinciaService;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CentroService")
class CentroServiceTest {

    @Mock private CentroRepository centroRepository;
    @Mock private CentroMapper centroMapper;
    @Mock private ProvinciaService provinciaService;
    @Mock private UsuarioMapper usuarioMapper;

    @InjectMocks
    private CentroService centroService;

    private Centro centro;
    private CentroDTO centroDTO;

    @BeforeEach
    void setUp() {
        centro = new Centro();
        centro.setId(1L);
        centro.setNombreComercial("Academia TestFP");
        centro.setCodigo("28000001");
        centro.setDireccion("Calle Mayor 1");
        centro.setLocalidad("Madrid");
        centro.setProvincia(new Provincia(28L, "Madrid", "28", new ComunidadAutonoma(13L, "Madrid", "13")));
        centro.setTipo(TipoCentro.PRIVADO);
        centro.setEmail("academia@test.com");
        centro.setVerificado(false);
        centro.setTieneGestor(false);
        centro.setGestores(new HashSet<>());
        centro.setFormaciones(new HashSet<>());
        centro.setSolicitudesGestion(new HashSet<>());

        centroDTO = new CentroDTO();
        centroDTO.setId(1L);
        centroDTO.setNombreComercial("Academia TestFP");
        centroDTO.setCodigo("28000001");
        centroDTO.setDireccion("Calle Mayor 1");
        centroDTO.setLocalidad("Madrid");
        centroDTO.setProvincia("Madrid");
        centroDTO.setTipo(TipoCentro.PRIVADO);
        centroDTO.setEmail("academia@test.com");
        centroDTO.setVerificado(false);
        centroDTO.setTieneGestor(false);
    }

    @Nested
    @DisplayName("crearCentro()")
    class CrearCentro {

        @Test
        @DisplayName("centro válido sin email persiste y retorna DTO")
        void centroValidoSinEmail_guardaYRetornaDTO() {
            centro.setEmail(null);
            when(centroRepository.existsByCodigo("28000001")).thenReturn(false);
            when(centroRepository.save(any(Centro.class))).thenReturn(centro);
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            CentroDTO resultado = centroService.crearCentro(centro);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo("28000001");
            verify(centroRepository).save(centro);
            verify(centroRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("centro válido con email comprueba unicidad y persiste")
        void centroValidoConEmail_compruebEmailYGuarda() {
            when(centroRepository.existsByCodigo("28000001")).thenReturn(false);
            when(centroRepository.existsByEmail("academia@test.com")).thenReturn(false);
            when(centroRepository.save(any(Centro.class))).thenReturn(centro);
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            CentroDTO resultado = centroService.crearCentro(centro);

            assertThat(resultado).isNotNull();
            verify(centroRepository).existsByEmail("academia@test.com");
        }

        @Test
        @DisplayName("centro nulo lanza IllegalArgumentException")
        void centroNulo_lanzaIllegalArgumentException() {
            assertThatThrownBy(() -> centroService.crearCentro(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Centro nulo");
            verifyNoInteractions(centroRepository);
        }

        @Test
        @DisplayName("código duplicado lanza IllegalArgumentException")
        void codigoDuplicado_lanzaIllegalArgumentException() {
            when(centroRepository.existsByCodigo("28000001")).thenReturn(true);

            assertThatThrownBy(() -> centroService.crearCentro(centro))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un centro con ese código");
            verify(centroRepository, never()).save(any());
        }

        @Test
        @DisplayName("email duplicado lanza IllegalArgumentException")
        void emailDuplicado_lanzaIllegalArgumentException() {
            when(centroRepository.existsByCodigo("28000001")).thenReturn(false);
            when(centroRepository.existsByEmail("academia@test.com")).thenReturn(true);

            assertThatThrownBy(() -> centroService.crearCentro(centro))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un centro con ese email");
            verify(centroRepository, never()).save(any());
        }

        @Test
        @DisplayName("registrarCentro desde DTO delega en crearCentro")
        void registrarCentroDesdeDTO_delegaEnCrearCentro() {
            RegistroCentroRequestDTO dto = new RegistroCentroRequestDTO();
            dto.setCodigo("28000001");
            dto.setNombreComercial("Academia TestFP");
            dto.setDireccion("Calle Mayor 1");
            dto.setLocalidad("Madrid");
            dto.setProvincia("Madrid");
            dto.setTipo(TipoCentro.PRIVADO);

            // Valida el flujo de mapeo: DTO -> Entidad -> Persistencia -> DTO sin acoplar lógica
            when(centroMapper.createCentroFromDTO(dto)).thenReturn(centro);
            when(centroRepository.existsByCodigo("28000001")).thenReturn(false);
            when(centroRepository.save(any())).thenReturn(centro);
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            CentroDTO resultado = centroService.registrarCentro(dto);

            assertThat(resultado).isNotNull();
            verify(centroMapper).createCentroFromDTO(dto);
            verify(centroRepository).save(any());
        }
    }

    @Nested
    @DisplayName("existeCodigo()")
    class ExisteCodigo {

        @Test
        @DisplayName("código existente retorna true")
        void codigoExistente_retornaTrue() {
            when(centroRepository.existsByCodigo("28000001")).thenReturn(true);
            assertThat(centroService.existeCodigo("28000001")).isTrue();
        }

        @Test
        @DisplayName("código no registrado retorna false")
        void codigoNoExistente_retornaFalse() {
            when(centroRepository.existsByCodigo("99000001")).thenReturn(false);
            assertThat(centroService.existeCodigo("99000001")).isFalse();
        }
    }

    @Nested
    @DisplayName("obtenerCentros()")
    class ObtenerCentros {

        @Test
        @DisplayName("retorna lista completa de centros mapeados")
        void retornaListaCompleta() {
            when(centroRepository.findAll()).thenReturn(List.of(centro));
            when(centroMapper.toDTOList(List.of(centro))).thenReturn(List.of(centroDTO));

            List<CentroDTO> resultado = centroService.obtenerCentros();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getCodigo()).isEqualTo("28000001");
        }

        @Test
        @DisplayName("obtenerCentrosVerificados retorna solo los verificados")
        void centrosVerificados_soloRetornaVerificados() {
            Centro centroVerificado = new Centro();
            centroVerificado.setVerificado(true);

            CentroDTO centroDTOVerificado = new CentroDTO();
            centroDTOVerificado.setVerificado(true);

            when(centroRepository.findByVerificadoTrue()).thenReturn(List.of(centroVerificado));
            when(centroMapper.toDTOList(List.of(centroVerificado))).thenReturn(List.of(centroDTOVerificado));

            List<CentroDTO> resultado = centroService.obtenerCentrosVerificados();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getVerificado()).isTrue();
        }

        @Test
        @DisplayName("obtenerCentrosNoVerificados retorna solo los no verificados")
        void centrosNoVerificados_soloRetornaNoVerificados() {
            when(centroRepository.findByVerificadoFalse()).thenReturn(List.of(centro));
            when(centroMapper.toDTOList(List.of(centro))).thenReturn(List.of(centroDTO));

            List<CentroDTO> resultado = centroService.obtenerCentrosNoVerificados();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getVerificado()).isFalse();
        }
    }

    @Nested
    @DisplayName("obtenerCentroPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("id existente retorna la entidad")
        void idExistente_retornaCentro() {
            when(centroRepository.findById(1L)).thenReturn(Optional.of(centro));

            Centro resultado = centroService.obtenerCentroPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("id inexistente lanza IllegalArgumentException")
        void idInexistente_lanzaIllegalArgumentException() {
            when(centroRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> centroService.obtenerCentroPorId(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("obtenerCentroDTOPorId mapea correctamente la entidad a DTO")
        void obtenerCentroDTOPorId_mapeaADTO() {
            when(centroRepository.findById(1L)).thenReturn(Optional.of(centro));
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            CentroDTO resultado = centroService.obtenerCentroDTOPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("obtenerCentroPorCodigo()")
    class ObtenerPorCodigo {

        @Test
        @DisplayName("código existente retorna la entidad")
        void codigoExistente_retornaCentro() {
            when(centroRepository.findByCodigo("28000001")).thenReturn(Optional.of(centro));

            Centro resultado = centroService.obtenerCentroPorCodigo("28000001");

            assertThat(resultado).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo("28000001");
        }

        @Test
        @DisplayName("código no registrado lanza IllegalArgumentException")
        void codigoNoExistente_lanzaIllegalArgumentException() {
            when(centroRepository.findByCodigo("99000001")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> centroService.obtenerCentroPorCodigo("99000001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99000001");
        }

        @Test
        @DisplayName("obtenerCentroDTOPorCodigo mapea correctamente la entidad a DTO")
        void obtenerCentroDTOPorCodigo_mapeaADTO() {
            when(centroRepository.findByCodigo("28000001")).thenReturn(Optional.of(centro));
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            CentroDTO resultado = centroService.obtenerCentroDTOPorCodigo("28000001");

            assertThat(resultado).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo("28000001");
        }
    }

    @Nested
    @DisplayName("actualizarCentro()")
    class ActualizarCentro {

        @Test
        @DisplayName("DTO nulo lanza IllegalArgumentException")
        void dtoNulo_lanzaIllegalArgumentException() {
            assertThatThrownBy(() -> centroService.actualizarCentro(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No se han recibido correctamente los nuevos datos");
        }

        @Test
        @DisplayName("centro inexistente lanza IllegalArgumentException")
        void centroNoExiste_lanzaIllegalArgumentException() {
            when(centroRepository.findById(999L)).thenReturn(Optional.empty());

            CentroUpdateDTO dto = new CentroUpdateDTO();
            dto.setCodigo("28000001");

            assertThatThrownBy(() -> centroService.actualizarCentro(999L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("mismo código actualiza sin verificar unicidad")
        void mismoCodigo_noVerificaUnicidad_actualiza() {
            CentroUpdateDTO dto = new CentroUpdateDTO();
            dto.setCodigo("28000001");
            dto.setNombreComercial("Nombre nuevo");

            // Simula la recuperación de la base de datos y valida que no intente comprobar unicidad si no cambió el código
            when(centroRepository.findById(1L)).thenReturn(Optional.of(centro));
            when(centroRepository.save(any())).thenReturn(centro);
            when(centroMapper.toDTO(any())).thenReturn(centroDTO);

            assertThatCode(() -> centroService.actualizarCentro(1L, dto))
                    .doesNotThrowAnyException();

            verify(centroRepository, never()).existsByCodigo(anyString());
            verify(centroRepository).save(centro);
        }

        @Test
        @DisplayName("código cambiado y duplicado lanza IllegalArgumentException")
        void codigoCambiado_duplicado_lanzaIllegalArgumentException() {
            CentroUpdateDTO dto = new CentroUpdateDTO();
            dto.setCodigo("28000002");

            when(centroRepository.findById(1L)).thenReturn(Optional.of(centro));
            when(centroRepository.existsByCodigo("28000002")).thenReturn(true);

            assertThatThrownBy(() -> centroService.actualizarCentro(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un centro con ese código");
        }

        @Test
        @DisplayName("email cambiado y duplicado lanza IllegalArgumentException")
        void emailCambiado_duplicado_lanzaIllegalArgumentException() {
            CentroUpdateDTO dto = new CentroUpdateDTO();
            dto.setCodigo("28000001");
            dto.setEmail("otro@test.com");

            when(centroRepository.findById(1L)).thenReturn(Optional.of(centro));
            when(centroRepository.existsByEmail("otro@test.com")).thenReturn(true);

            assertThatThrownBy(() -> centroService.actualizarCentro(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un centro con ese email");
        }
    }

    @Nested
    @DisplayName("verificarCentro() / quitarVerificacionCentro()")
    class VerificacionCentro {

        @Test
        @DisplayName("verificarCentro establece verificado=true y guarda fecha")
        void verificarCentro_poneTrueYGuardaFecha() {
            when(centroRepository.findById(1L)).thenReturn(Optional.of(centro));
            when(centroRepository.save(any())).thenReturn(centro);

            assertThatCode(() -> centroService.verificarCentro(1L))
                    .doesNotThrowAnyException();

            assertThat(centro.getVerificado()).isTrue();
            assertThat(centro.getFechaVerificacion()).isNotNull();
            verify(centroRepository).save(centro);
        }

        @Test
        @DisplayName("verificarCentro con centro inexistente lanza IllegalArgumentException")
        void verificarCentro_centroInexistente_lanzaException() {
            when(centroRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> centroService.verificarCentro(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("quitarVerificacionCentro establece verificado=false y guarda")
        void quitarVerificacion_poneFalse() {
            centro.setVerificado(true);
            when(centroRepository.findById(1L)).thenReturn(Optional.of(centro));
            when(centroRepository.save(any())).thenReturn(centro);

            assertThatCode(() -> centroService.quitarVerificacionCentro(1L))
                    .doesNotThrowAnyException();

            assertThat(centro.getVerificado()).isFalse();
            verify(centroRepository).save(centro);
        }
    }

    @Nested
    @DisplayName("eliminarCentro()")
    class EliminarCentro {

        @Test
        @DisplayName("id existente invoca deleteById")
        void idExistente_eliminaCorrectamente() {
            when(centroRepository.existsById(1L)).thenReturn(true);

            assertThatCode(() -> centroService.eliminarCentro(1L))
                    .doesNotThrowAnyException();

            verify(centroRepository).deleteById(1L);
        }

        @Test
        @DisplayName("id inexistente lanza IllegalStateException sin llamar a deleteById")
        void idInexistente_lanzaIllegalStateException() {
            when(centroRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> centroService.eliminarCentro(999L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("999");

            verify(centroRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("buscarVerificadosConFiltros()")
    class BuscarVerificadosConFiltros {

        @Test
        @DisplayName("con término de búsqueda normaliza y devuelve página")
        void conTermino_normalizaYRetornaPagina() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Centro> pageCentros = new PageImpl<>(List.of(centro));
            when(centroRepository.buscarVerificadosConFiltros(eq(1L), eq("academia"), isNull(), isNull(), eq(pageable)))
                    .thenReturn(pageCentros);
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            Page<CentroDTO> resultado = centroService.buscarVerificadosConFiltros(1L, "  Academia  ", null, null, pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("sin término usa cadena vacía")
        void sinTermino_usaCadenaVacia() {
            Pageable pageable = PageRequest.of(0, 10);
            when(centroRepository.buscarVerificadosConFiltros(isNull(), eq(""), isNull(), isNull(), eq(pageable)))
                    .thenReturn(Page.empty());

            Page<CentroDTO> resultado = centroService.buscarVerificadosConFiltros(null, null, null, null, pageable);

            assertThat(resultado.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("buscarSinVerificarConFiltros()")
    class BuscarSinVerificarConFiltros {

        @Test
        @DisplayName("con tipo filtra por tipo y devuelve página")
        void conTipo_retornaPaginaFiltrada() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<Centro> pageCentros = new PageImpl<>(List.of(centro));
            when(centroRepository.buscarSinVerificarConFiltros(isNull(), eq(""), eq(TipoCentro.PRIVADO), eq(pageable)))
                    .thenReturn(pageCentros);
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            Page<CentroDTO> resultado = centroService.buscarSinVerificarConFiltros(null, null, TipoCentro.PRIVADO, pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("obtenerCentrosVerificadosPaginados()")
    class ObtenerVerificadosPaginados {

        @Test
        @DisplayName("retorna página de centros verificados")
        void retornaPaginaVerificados() {
            Pageable pageable = PageRequest.of(0, 10);
            centro.setVerificado(true);
            Page<Centro> pageCentros = new PageImpl<>(List.of(centro));
            when(centroRepository.findByVerificadoTrue(pageable)).thenReturn(pageCentros);
            centroDTO.setVerificado(true);
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            Page<CentroDTO> resultado = centroService.obtenerCentrosVerificadosPaginados(pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
            assertThat(resultado.getContent().get(0).getVerificado()).isTrue();
        }
    }

    @Nested
    @DisplayName("obtenerCentrosNoVerificadosPaginados()")
    class ObtenerNoVerificadosPaginados {

        @Test
        @DisplayName("retorna página de centros sin verificar")
        void retornaPaginaNoVerificados() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Centro> pageCentros = new PageImpl<>(List.of(centro));
            when(centroRepository.findByVerificadoFalse(pageable)).thenReturn(pageCentros);
            when(centroMapper.toDTO(centro)).thenReturn(centroDTO);

            Page<CentroDTO> resultado = centroService.obtenerCentrosNoVerificadosPaginados(pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("obtenerGestoresDeCentro()")
    class ObtenerGestoresDeCentro {

        @Test
        @DisplayName("retorna lista de gestores mapeados con todos los elementos")
        void centroConGestores_retornaListaCompleta() {
            Usuario gestor1 = new Usuario();
            gestor1.setId(1L);
            Usuario gestor2 = new Usuario();
            gestor2.setId(2L);
            centro.setGestores(Set.of(gestor1, gestor2));

            UsuarioDTO dto1 = new UsuarioDTO();
            dto1.setNombre("Ana");
            dto1.setApellidos("Garcia");
            UsuarioDTO dto2 = new UsuarioDTO();
            dto2.setNombre("Pedro");
            dto2.setApellidos("Benitez");

            when(centroRepository.findById(1L)).thenReturn(Optional.of(centro));
            when(usuarioMapper.toDTO(gestor1)).thenReturn(dto1);
            when(usuarioMapper.toDTO(gestor2)).thenReturn(dto2);

            List<UsuarioDTO> resultado = centroService.obtenerGestoresDeCentro(1L);

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).getApellidos()).isEqualTo("Benitez");
        }

        @Test
        @DisplayName("centro inexistente lanza IllegalArgumentException")
        void centroInexistente_lanzaIllegalArgumentException() {
            when(centroRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> centroService.obtenerGestoresDeCentro(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }
}
