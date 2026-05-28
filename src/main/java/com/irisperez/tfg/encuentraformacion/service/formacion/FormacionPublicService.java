package com.irisperez.tfg.encuentraformacion.service.formacion;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionDetalleDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionFiltroDTO;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.mapper.formacion.FormacionMapper;
import com.irisperez.tfg.encuentraformacion.mapper.valoracion.ValoracionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import com.irisperez.tfg.encuentraformacion.model.entity.Valoracion;
import com.irisperez.tfg.encuentraformacion.repository.spec.FormacionEspecificaciones;
import com.irisperez.tfg.encuentraformacion.repository.FormacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.ValoracionRepository;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormacionPublicService {

    private final FormacionRepository formacionRepository;
    private final FormacionMapper formacionMapper;
    private final ValoracionRepository valoracionRepository;
    private final ValoracionMapper valoracionMapper;
    private final ValoracionEstudianteService valoracionService;
    private final EntityManager entityManager;

    public Page<FormacionResumenDTO> buscar(FormacionFiltroDTO filtro, Pageable pageable, String sortBy) {
        Page<FormacionResumenDTO> page;

        if ("valoracion".equals(sortBy)) {
            boolean soloGratuitas = filtro != null && Boolean.TRUE.equals(filtro.getSoloGratuitas());
            String nombre         = normalizar(filtro != null ? filtro.getNombre() : null);
            String tituloOficial  = normalizar(filtro != null ? filtro.getTituloOficial() : null);
            String localidad      = normalizar(filtro != null ? filtro.getLocalidad() : null);
            page = formacionRepository.findOrdenadosPorValoracion(
                    nombre != null, nombre != null ? nombre : "",
                    tituloOficial != null, tituloOficial != null ? tituloOficial : "",
                    localidad != null, localidad != null ? localidad : "",
                    filtro != null ? filtro.getModalidad() : null,
                    filtro != null ? filtro.getProvinciaId() : null,
                    filtro != null ? filtro.getHorario() : null,
                    filtro != null ? filtro.getTipoEstudiosId() : null,
                    filtro != null ? filtro.getTipoCentro() : null,
                    soloGratuitas,
                    soloGratuitas ? null : (filtro != null ? filtro.getPrecioMin() : null),
                    soloGratuitas ? null : (filtro != null ? filtro.getPrecioMax() : null),
                    filtro != null ? filtro.getFechaInicioDesde() : null,
                    pageable
            ).map(formacionMapper::toResumenDTO);
        } else {
            page = formacionRepository
                    .findAll(FormacionEspecificaciones.conFiltros(filtro), pageable)
                    .map(formacionMapper::toResumenDTO);
        }

        valoracionService.cargarValoraciones(page.getContent());
        return page;
    }

    public List<String> buscarTitulosOficiales(String q) {
        String qNorm = q == null ? "" : sinAcentos(q.trim());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Formacion> f = cq.from(Formacion.class);
        Join<Formacion, Centro> c = f.join("centro", JoinType.INNER);

        cq.select(f.get("tituloOficial")).distinct(true)
          .where(
              cb.isTrue(f.get("activa")),
              cb.isTrue(c.get("verificado")),
              cb.isNotNull(f.get("tituloOficial")),
              cb.notEqual(f.get("tituloOficial"), ""),
              cb.like(
                  cb.function("unaccent", String.class, cb.lower(f.get("tituloOficial"))),
                  "%" + qNorm + "%"
              )
          )
          .orderBy(cb.asc(f.get("tituloOficial")));

        return entityManager.createQuery(cq).setMaxResults(20).getResultList();
    }

    public FormacionDetalleDTO findByUuid(UUID uuid) {
        Formacion formacion = formacionRepository.findByUuid(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Formación no encontrada"));

        FormacionDetalleDTO dto = formacionMapper.toDetalleDTO(formacion);
        List<Valoracion> valoraciones = valoracionRepository.findByFormacionIdConEstudiante(formacion.getId());
        dto.setValoraciones(valoracionMapper.toDTOList(valoraciones));
        dto.setTotalValoraciones((long) valoraciones.size());
        dto.setValoracionMedia(valoraciones.isEmpty() ? null :
            valoraciones.stream().mapToInt(Valoracion::getEstrellas).average().orElse(0.0));
        return dto;
    }

    private static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return sinAcentos(texto.trim());
    }

    private static String sinAcentos(String texto) {
        return Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
