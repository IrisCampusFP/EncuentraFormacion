package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionFiltroDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.enums.HorarioFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.ModalidadFormacion;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.repository.spec.FormacionEspecificaciones;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("FormacionRepository")
class FormacionRepositoryTest {

    @Autowired private FormacionRepository formacionRepository;
    @Autowired private CentroRepository centroRepository;
    @Autowired private ProvinciaRepository provinciaRepository;
    @Autowired private ComunidadAutonomaRepository comunidadAutonomaRepository;

    private Centro centro;
    private Formacion formacionActiva;
    private Formacion formacionInactiva;

    @BeforeEach
    void setUp() {
        ComunidadAutonoma ccaa = comunidadAutonomaRepository.findByNombreIgnoreCase("Madrid")
            .orElseGet(() -> comunidadAutonomaRepository.save(new ComunidadAutonoma(null, "Madrid", "13")));
        Provincia madrid = provinciaRepository.findByNombreIgnoreCase("Madrid")
            .orElseGet(() -> provinciaRepository.save(new Provincia(null, "Madrid", "28", ccaa)));

        centro = new Centro();
        centro.setNombreComercial("Academia Test FR");
        centro.setCodigo("28000003");
        centro.setDireccion("Calle Test FR 1");
        centro.setLocalidad("Madrid");
        centro.setProvincia(madrid);
        centro.setTipo(TipoCentro.PRIVADO);
        centro.setVerificado(true);
        centroRepository.saveAndFlush(centro);

        formacionActiva = new Formacion();
        formacionActiva.setCentro(centro);
        formacionActiva.setNombre("Curso Activo FR");
        formacionActiva.setModalidad(ModalidadFormacion.PRESENCIAL);
        formacionActiva.setHorario(HorarioFormacion.MANANA);
        formacionActiva.setActiva(true);
        formacionActiva.setPrecio(new BigDecimal("300.00"));
        formacionRepository.saveAndFlush(formacionActiva);

        formacionInactiva = new Formacion();
        formacionInactiva.setCentro(centro);
        formacionInactiva.setNombre("Curso Inactivo FR");
        formacionInactiva.setModalidad(ModalidadFormacion.DISTANCIA);
        formacionInactiva.setHorario(HorarioFormacion.TARDE);
        formacionInactiva.setActiva(false);
        formacionRepository.saveAndFlush(formacionInactiva);
    }

    @Test
    @DisplayName("findByIdAndActivaTrue no devuelve formaciones inactivas")
    void findByIdAndActivaTrue_inactiva_devuelveVacio() {
        Optional<Formacion> result = formacionRepository.findByIdAndActivaTrue(formacionInactiva.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndActivaTrue devuelve la formación activa")
    void findByIdAndActivaTrue_activa_devuelveFormacion() {
        Optional<Formacion> result = formacionRepository.findByIdAndActivaTrue(formacionActiva.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getNombre()).isEqualTo("Curso Activo FR");
    }

    @Test
    @DisplayName("Specification filtra por nombre parcial (case-insensitive)")
    void specification_filtraPorNombreParcial() {
        FormacionFiltroDTO filtro = new FormacionFiltroDTO();
        filtro.setNombre("activo fr");
        Page<Formacion> result = formacionRepository.findAll(
            FormacionEspecificaciones.conFiltros(filtro), PageRequest.of(0, 10)
        );
        assertThat(result.getContent()).extracting(Formacion::getNombre)
            .contains("Curso Activo FR");
    }

    @Test
    @DisplayName("Specification solo devuelve formaciones activas")
    void specification_soloDevuelveActivas() {
        FormacionFiltroDTO filtro = new FormacionFiltroDTO();
        filtro.setNombre("fr");  // filtramos por nuestros datos de prueba
        Page<Formacion> result = formacionRepository.findAll(
            FormacionEspecificaciones.conFiltros(filtro), PageRequest.of(0, 10)
        );
        assertThat(result.getContent()).allMatch(Formacion::getActiva);
    }

    @Test
    @DisplayName("Specification filtra por modalidad")
    void specification_filtraPorModalidad() {
        FormacionFiltroDTO filtro = new FormacionFiltroDTO();
        filtro.setNombre("fr");
        filtro.setModalidad(ModalidadFormacion.PRESENCIAL);
        Page<Formacion> result = formacionRepository.findAll(
            FormacionEspecificaciones.conFiltros(filtro), PageRequest.of(0, 10)
        );
        assertThat(result.getContent()).allMatch(f -> f.getModalidad() == ModalidadFormacion.PRESENCIAL);
    }

    @Test
    @DisplayName("findActivasByCentroIdConCentro solo devuelve activas del centro")
    void findActivasByCentroId_soloActivas() {
        List<Formacion> result = formacionRepository.findActivasByCentroIdConCentro(centro.getId());
        assertThat(result).extracting(Formacion::getNombre).contains("Curso Activo FR");
        assertThat(result).allMatch(Formacion::getActiva);
    }
}
