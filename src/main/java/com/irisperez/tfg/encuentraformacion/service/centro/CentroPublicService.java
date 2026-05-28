package com.irisperez.tfg.encuentraformacion.service.centro;

import com.irisperez.tfg.encuentraformacion.dto.centro.CentroBuscadorDTO;
import com.irisperez.tfg.encuentraformacion.dto.centro.CentroPerfilDTO;
import com.irisperez.tfg.encuentraformacion.mapper.faq.FaqCentroMapper;
import com.irisperez.tfg.encuentraformacion.mapper.formacion.FormacionMapper;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import com.irisperez.tfg.encuentraformacion.repository.CentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.FaqCentroRepository;
import com.irisperez.tfg.encuentraformacion.repository.FormacionRepository;
import com.irisperez.tfg.encuentraformacion.repository.ValoracionRepository;
import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionResumenDTO;
import com.irisperez.tfg.encuentraformacion.repository.projection.CentroValoracionStatsProjection;
import com.irisperez.tfg.encuentraformacion.service.valoracion.ValoracionEstudianteService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CentroPublicService {

    private final CentroRepository centroRepository;
    private final FormacionRepository formacionRepository;
    private final ValoracionRepository valoracionRepository;
    private final FaqCentroRepository faqCentroRepository;
    private final FormacionMapper formacionMapper;
    private final FaqCentroMapper faqCentroMapper;
    private final ValoracionEstudianteService valoracionService;

    public Page<CentroBuscadorDTO> buscarCentros(String nombre, String localidad, Long provinciaId,
                                                  TipoCentro tipo, String sortBy, Pageable pageable) {
        String nombreFiltro    = normalizar(nombre);
        String localidadFiltro = normalizar(localidad);

        Page<Centro> pagina;
        if ("valorados".equals(sortBy)) {
            pagina = centroRepository.findVerificadosOrderByValoracion(
                    nombreFiltro != null, nombreFiltro != null ? nombreFiltro : "",
                    localidadFiltro != null, localidadFiltro != null ? localidadFiltro : "",
                    provinciaId, tipo, pageable);
        } else {
            pagina = centroRepository.findAll(construirSpec(nombreFiltro, localidadFiltro, provinciaId, tipo), pageable);
        }

        List<Long> centroIds = pagina.getContent().stream().map(Centro::getId).collect(Collectors.toList());
        Map<Long, CentroValoracionStatsProjection> statsMap = valoracionRepository
                .findStatsByCentroIds(centroIds).stream()
                .collect(Collectors.toMap(CentroValoracionStatsProjection::getCentroId, s -> s));
        Map<Long, Integer> formacionesMap = formacionRepository
                .countActivasByCentroIds(centroIds).stream()
                .collect(Collectors.toMap(
                    r -> r.getCentroId(),
                    r -> r.getTotal().intValue()));

        return pagina.map(c -> mapearDTO(c, statsMap.get(c.getId()), formacionesMap.getOrDefault(c.getId(), 0)));
    }

    private Specification<Centro> construirSpec(String nombre, String localidad, Long provinciaId, TipoCentro tipo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("verificado")));
            if (nombre != null) {
                predicates.add(cb.like(
                    cb.function("unaccent", String.class, cb.lower(root.get("nombreComercial"))),
                    "%" + nombre + "%"));
            }
            if (localidad != null) {
                predicates.add(cb.like(
                    cb.function("unaccent", String.class, cb.lower(root.get("localidad"))),
                    "%" + localidad + "%"));
            }
            if (provinciaId != null) {
                predicates.add(cb.equal(root.get("provincia").get("id"), provinciaId));
            }
            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private CentroBuscadorDTO mapearDTO(Centro c, CentroValoracionStatsProjection stats, int totalFormaciones) {
        CentroBuscadorDTO dto = new CentroBuscadorDTO();
        dto.setUuid(c.getUuid());
        dto.setNombreComercial(c.getNombreComercial());
        dto.setDescripcion(c.getDescripcion());
        dto.setDireccion(c.getDireccion());
        dto.setLocalidad(c.getLocalidad());
        dto.setProvincia(c.getProvincia() != null ? c.getProvincia().getNombre() : null);
        dto.setTipo(c.getTipo());
        dto.setTelefono(c.getTelefono());
        dto.setEmail(c.getEmail());
        dto.setPaginaWeb(c.getPaginaWeb());
        dto.setValoracionMedia(stats != null ? stats.getMedia() : null);
        dto.setTotalValoraciones(stats != null ? stats.getTotal().intValue() : 0);
        dto.setTotalFormaciones(totalFormaciones);
        return dto;
    }

    public Page<FormacionResumenDTO> getFormacionesPaginadas(UUID uuid, Pageable pageable) {
        Centro centro = centroRepository.findByUuid(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centro no encontrado"));
        Page<FormacionResumenDTO> resultado = formacionRepository
            .findActivasByCentroIdPageable(centro.getId(), pageable)
            .map(formacionMapper::toResumenDTO);
        valoracionService.cargarValoraciones(resultado.getContent());
        return resultado;
    }

    public CentroPerfilDTO getPerfilByUuid(UUID uuid) {
        Centro centro = centroRepository.findByUuid(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Centro no encontrado"));

        Long id = centro.getId();
        CentroPerfilDTO dto = new CentroPerfilDTO();
        dto.setId(centro.getId());
        dto.setUuid(centro.getUuid());
        dto.setNombreComercial(centro.getNombreComercial());
        dto.setDescripcion(centro.getDescripcion());
        dto.setDireccion(centro.getDireccion());
        dto.setLocalidad(centro.getLocalidad());
        dto.setProvincia(centro.getProvincia() != null ? centro.getProvincia().getNombre() : null);
        dto.setTipo(centro.getTipo());
        dto.setTelefono(centro.getTelefono());
        dto.setEmail(centro.getEmail());
        dto.setPaginaWeb(centro.getPaginaWeb());
        dto.setVerificado(centro.getVerificado());

        dto.setValoracionMedia(valoracionRepository.findMediaByCentroId(id));
        dto.setTotalValoraciones(valoracionRepository.countByCentroId(id).intValue());

        List<FormacionResumenDTO> formaciones =
            formacionMapper.toResumenDTOList(formacionRepository.findActivasByCentroIdConCentro(id));
        valoracionService.cargarValoraciones(formaciones);
        dto.setFormaciones(formaciones);
        dto.setFaqs(faqCentroMapper.toDTOList(
            faqCentroRepository.findByCentroIdOrderByOrdenAsc(id)
        ));

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
