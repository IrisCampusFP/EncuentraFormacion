package com.irisperez.tfg.encuentraformacion.repository.spec;

import com.irisperez.tfg.encuentraformacion.dto.formacion.FormacionFiltroDTO;
import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.entity.Formacion;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class FormacionEspecificaciones {

    private FormacionEspecificaciones() {}

    // Specification que añade ORDER BY media de valoraciones DESC, total DESC
    // usando subqueries de agregación — PostgreSQL ordena y pagina en una sola query
    private static String sinAcentos(String texto) {
        if (texto == null) return null;
        return Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    public static Specification<Formacion> conFiltros(FormacionFiltroDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("activa")));

            final Join<Formacion, Centro> centroJoin;
            if (!Long.class.equals(query.getResultType())) {
                centroJoin = (Join<Formacion, Centro>) (Object) root.fetch("centro", JoinType.INNER);
                root.fetch("tipoEstudios", JoinType.LEFT);
                query.distinct(true);
            } else {
                centroJoin = root.join("centro", JoinType.INNER);
            }

            predicates.add(cb.isTrue(centroJoin.get("verificado")));

            if (filtro != null) {
                if (filtro.getNombres() != null && !filtro.getNombres().isEmpty()) {
                    List<String> terminos = filtro.getNombres().stream()
                        .filter(n -> n != null && !n.isBlank())
                        .map(FormacionEspecificaciones::sinAcentos)
                        .toList();
                    if (!terminos.isEmpty()) {
                        Predicate[] orTerminos = terminos.stream()
                            .map(t -> cb.like(
                                cb.function("unaccent", String.class, cb.lower(root.get("nombre"))),
                                "%" + t + "%"
                            ))
                            .toArray(Predicate[]::new);
                        predicates.add(cb.or(orTerminos));
                    }
                } else if (filtro.getNombre() != null && !filtro.getNombre().isBlank()) {
                    predicates.add(cb.like(
                        cb.function("unaccent", String.class, cb.lower(root.get("nombre"))),
                        "%" + sinAcentos(filtro.getNombre()) + "%"
                    ));
                }
                if (filtro.getTituloOficial() != null && !filtro.getTituloOficial().isBlank()) {
                    predicates.add(cb.like(
                        cb.function("unaccent", String.class, cb.lower(root.get("tituloOficial"))),
                        "%" + sinAcentos(filtro.getTituloOficial()) + "%"
                    ));
                }
                if (filtro.getModalidad() != null) {
                    predicates.add(cb.equal(root.get("modalidad"), filtro.getModalidad()));
                }
                if (filtro.getComunidadAutonomaId() != null) {
                    predicates.add(cb.equal(
                        centroJoin.get("provincia").get("comunidadAutonoma").get("id"),
                        filtro.getComunidadAutonomaId()
                    ));
                }
                if (filtro.getProvinciaId() != null) {
                    predicates.add(cb.equal(
                        centroJoin.get("provincia").get("id"), filtro.getProvinciaId()
                    ));
                }
                if (filtro.getLocalidad() != null && !filtro.getLocalidad().isBlank()) {
                    predicates.add(cb.like(
                        cb.function("unaccent", String.class, cb.lower(centroJoin.get("localidad"))),
                        "%" + sinAcentos(filtro.getLocalidad()) + "%"
                    ));
                }
                if (filtro.getHorario() != null) {
                    predicates.add(cb.equal(root.get("horario"), filtro.getHorario()));
                }
                if (filtro.getTipoEstudiosId() != null) {
                    predicates.add(cb.equal(
                        root.get("tipoEstudios").get("id"), filtro.getTipoEstudiosId()
                    ));
                }
                if (filtro.getTipoCentro() != null) {
                    predicates.add(cb.equal(centroJoin.get("tipo"), filtro.getTipoCentro()));
                }
                if (Boolean.TRUE.equals(filtro.getSoloGratuitas())) {
                    predicates.add(cb.or(
                        cb.isNull(root.get("precio")),
                        cb.equal(root.get("precio"), BigDecimal.ZERO)
                    ));
                } else {
                    if (filtro.getPrecioMin() != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("precio"), filtro.getPrecioMin()));
                    }
                    if (filtro.getPrecioMax() != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("precio"), filtro.getPrecioMax()));
                    }
                }
                if (filtro.getFechaInicioDesde() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("fechaInicio"), filtro.getFechaInicioDesde()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
