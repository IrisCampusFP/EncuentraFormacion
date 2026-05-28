package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import com.irisperez.tfg.encuentraformacion.model.enums.EstadoSolicitud;
import com.irisperez.tfg.encuentraformacion.model.entity.Provincia;
import com.irisperez.tfg.encuentraformacion.model.entity.SolicitudGestion;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("SolicitudGestionRepository")
class SolicitudGestionRepositoryTest {

    @Autowired
    private SolicitudGestionRepository solicitudRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CentroRepository centroRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private ComunidadAutonomaRepository comunidadAutonomaRepository;

    private Usuario usuario;
    private Centro centro;
    private SolicitudGestion solicitud;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setEmail("solicitante@test.com");
        usuario.setUsername("solicitante");
        usuario.setNombre("Luis");
        usuario.setApellidos("Pérez");
        usuario.setPassword("$2a$10$hashed");
        usuario.setActivo(true);
        usuario = usuarioRepository.saveAndFlush(usuario);

        ComunidadAutonoma ccaa = comunidadAutonomaRepository.findByNombreIgnoreCase("Madrid")
            .orElseGet(() -> comunidadAutonomaRepository.save(new ComunidadAutonoma(null, "Madrid", "13")));
        Provincia madrid = provinciaRepository.findByNombreIgnoreCase("Madrid")
            .orElseGet(() -> provinciaRepository.save(new Provincia(null, "Madrid", "28", ccaa)));

        centro = new Centro();
        centro.setNombreComercial("Academia H2 Test");
        centro.setCodigo("33000001");
        centro.setDireccion("Calle Centro 1");
        centro.setLocalidad("Madrid");
        centro.setProvincia(madrid);
        centro.setTipo(TipoCentro.PRIVADO);
        centro.setVerificado(false);
        centro = centroRepository.saveAndFlush(centro);

        solicitud = new SolicitudGestion();
        solicitud.setUsuario(usuario);
        solicitud.setCentro(centro);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setPruebaTitularidad(new byte[]{1, 2, 3});
        solicitud = solicitudRepository.saveAndFlush(solicitud);
    }

    @Test
    @DisplayName("findByEstado retorna solicitudes con el estado especificado")
    void findByEstado_RetornaSolicitudes() {
        List<SolicitudGestion> pendientes = solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE);

        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.get(0).getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    @DisplayName("findTopByUsuarioOrderByFechaSolicitudDesc retorna la solicitud más reciente del usuario")
    void findTopByUsuario_RetornaSolicitudMasReciente() {
        SolicitudGestion encontrada = solicitudRepository.findTopByUsuarioOrderByFechaSolicitudDesc(usuario);

        assertThat(encontrada).isNotNull();
        assertThat(encontrada.getUsuario().getEmail()).isEqualTo("solicitante@test.com");
    }

    @Test
    @DisplayName("obtenerHistorialSolicitudes retorna solicitudes no pendientes ordenadas por fecha desc")
    void obtenerHistorialSolicitudes_RetornaNoPendientesOrdenadas() {
        SolicitudGestion aceptada = new SolicitudGestion();
        aceptada.setUsuario(usuario);
        aceptada.setCentro(centro);
        aceptada.setEstado(EstadoSolicitud.ACEPTADA);
        aceptada.setFechaSolicitud(LocalDateTime.now().minusDays(1));
        aceptada.setPruebaTitularidad(new byte[]{4, 5, 6});
        solicitudRepository.saveAndFlush(aceptada);

        SolicitudGestion rechazada = new SolicitudGestion();
        rechazada.setUsuario(usuario);
        rechazada.setCentro(centro);
        rechazada.setEstado(EstadoSolicitud.RECHAZADA);
        rechazada.setFechaSolicitud(LocalDateTime.now());
        rechazada.setPruebaTitularidad(new byte[]{7, 8, 9});
        solicitudRepository.saveAndFlush(rechazada);

        List<SolicitudGestion> historial = solicitudRepository.obtenerHistorialSolicitudes(EstadoSolicitud.PENDIENTE);

        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(historial.get(1).getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);
    }
}
