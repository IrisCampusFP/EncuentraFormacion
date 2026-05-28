package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
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
@DisplayName("CentroRepository")
class CentroRepositoryTest {

    @Autowired
    private CentroRepository centroRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private ComunidadAutonomaRepository comunidadAutonomaRepository;

    private Centro centro;

    @BeforeEach
    void setUp() {
        ComunidadAutonoma ccaa = comunidadAutonomaRepository.findByNombreIgnoreCase("Madrid")
            .orElseGet(() -> comunidadAutonomaRepository.save(new ComunidadAutonoma(null, "Madrid", "13")));
        Provincia madrid = provinciaRepository.findByNombreIgnoreCase("Madrid")
            .orElseGet(() -> provinciaRepository.save(new Provincia(null, "Madrid", "28", ccaa)));

        centro = new Centro();
        centro.setNombreComercial("Academia H2 Test");
        centro.setCodigo("28000001");
        centro.setDireccion("Calle Falsa 123");
        centro.setLocalidad("Madrid");
        centro.setProvincia(madrid);
        centro.setTipo(TipoCentro.PRIVADO);
        centro.setEmail("h2@test.com");
        centro.setVerificado(false);
        centroRepository.saveAndFlush(centro);
    }

    @Test
    @DisplayName("findByCodigo retorna el centro cuando existe")
    void findByCodigo_CuandoExiste_RetornaCentro() {
        Optional<Centro> encontrado = centroRepository.findByCodigo("28000001");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombreComercial()).isEqualTo("Academia H2 Test");
    }

    @Test
    @DisplayName("findByCodigo retorna vacío cuando no existe")
    void findByCodigo_CuandoNoExiste_RetornaVacio() {
        Optional<Centro> encontrado = centroRepository.findByCodigo("99000099");

        assertThat(encontrado).isEmpty();
    }

    @Test
    @DisplayName("existsByCodigo retorna true si existe")
    void existsByCodigo_CuandoExiste_RetornaTrue() {
        boolean existe = centroRepository.existsByCodigo("28000001");

        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("existsByEmail retorna true si existe")
    void existsByEmail_CuandoExiste_RetornaTrue() {
        boolean existe = centroRepository.existsByEmail("h2@test.com");

        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("findByVerificadoTrue retorna solo centros verificados")
    void findByVerificadoTrue_RetornaVerificados() {
        ComunidadAutonoma ccaa = comunidadAutonomaRepository.findByNombreIgnoreCase("Madrid")
            .orElseGet(() -> comunidadAutonomaRepository.save(new ComunidadAutonoma(null, "Madrid", "13")));
        Provincia madrid = provinciaRepository.findByNombreIgnoreCase("Madrid")
            .orElseGet(() -> provinciaRepository.save(new Provincia(null, "Madrid", "28", ccaa)));

        Centro centroVerificado = new Centro();
        centroVerificado.setNombreComercial("Academia Verificada");
        centroVerificado.setCodigo("28000002");
        centroVerificado.setDireccion("Calle 2");
        centroVerificado.setLocalidad("Madrid");
        centroVerificado.setProvincia(madrid);
        centroVerificado.setTipo(TipoCentro.PUBLICO);
        centroVerificado.setEmail("verif@test.com");
        centroVerificado.setVerificado(true);
        centroRepository.saveAndFlush(centroVerificado);

        List<Centro> verificados = centroRepository.findByVerificadoTrue();

        assertThat(verificados).hasSize(1);
        assertThat(verificados.get(0).getCodigo()).isEqualTo("28000002");
    }

    @Test
    @DisplayName("findByVerificadoFalse retorna solo centros no verificados")
    void findByVerificadoFalse_RetornaNoVerificados() {
        List<Centro> noVerificados = centroRepository.findByVerificadoFalse();

        assertThat(noVerificados).hasSize(1);
        assertThat(noVerificados.get(0).getCodigo()).isEqualTo("28000001");
    }
}
