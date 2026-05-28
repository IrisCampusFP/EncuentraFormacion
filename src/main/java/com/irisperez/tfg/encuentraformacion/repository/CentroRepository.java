package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Centro;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoCentro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CentroRepository extends JpaRepository<Centro, Long>, JpaSpecificationExecutor<Centro> {

    // Busca un centro por su código oficial, útil para validar duplicados al registrar
    Optional<Centro> findByCodigo(String codigo);

    // Busca por UUID público, usado en URLs amigables
    Optional<Centro> findByUuid(UUID uuid);

    // Comprueba si el código oficial ya está en uso antes de crear un centro nuevo
    boolean existsByCodigo(String codigo);

    // Comprueba si el email ya está registrado
    boolean existsByEmail(String email);

    List<Centro> findByVerificadoTrue();
    Page<Centro> findByVerificadoTrue(Pageable pageable);

    List<Centro> findByVerificadoFalse();
    Page<Centro> findByVerificadoFalse(Pageable pageable);

    @Query(value = """
            SELECT c FROM Centro c
            WHERE c.verificado = true
              AND (:id IS NULL OR c.id = :id)
              AND (:q = '' OR LOWER(c.nombreComercial)  LIKE CONCAT('%', :q, '%')
                           OR LOWER(c.localidad)        LIKE CONCAT('%', :q, '%')
                           OR LOWER(c.provincia.nombre) LIKE CONCAT('%', :q, '%'))
              AND (:tipo        IS NULL OR c.tipo        = :tipo)
              AND (:tieneGestor IS NULL OR c.tieneGestor = :tieneGestor)
            """,
           countQuery = """
            SELECT COUNT(c) FROM Centro c
            WHERE c.verificado = true
              AND (:id IS NULL OR c.id = :id)
              AND (:q = '' OR LOWER(c.nombreComercial)  LIKE CONCAT('%', :q, '%')
                           OR LOWER(c.localidad)        LIKE CONCAT('%', :q, '%')
                           OR LOWER(c.provincia.nombre) LIKE CONCAT('%', :q, '%'))
              AND (:tipo        IS NULL OR c.tipo        = :tipo)
              AND (:tieneGestor IS NULL OR c.tieneGestor = :tieneGestor)
            """)
    Page<Centro> buscarVerificadosConFiltros(
            @Param("id")          Long      id,
            @Param("q")           String    q,
            @Param("tipo")        TipoCentro tipo,
            @Param("tieneGestor") Boolean   tieneGestor,
            Pageable pageable);

    @Query(value = """
            SELECT c FROM Centro c
            WHERE c.verificado = false
              AND (:id IS NULL OR c.id = :id)
              AND (:q = '' OR LOWER(c.nombreComercial)  LIKE CONCAT('%', :q, '%')
                           OR LOWER(c.localidad)        LIKE CONCAT('%', :q, '%')
                           OR LOWER(c.provincia.nombre) LIKE CONCAT('%', :q, '%'))
              AND (:tipo IS NULL OR c.tipo = :tipo)
            """,
           countQuery = """
            SELECT COUNT(c) FROM Centro c
            WHERE c.verificado = false
              AND (:id IS NULL OR c.id = :id)
              AND (:q = '' OR LOWER(c.nombreComercial)  LIKE CONCAT('%', :q, '%')
                           OR LOWER(c.localidad)        LIKE CONCAT('%', :q, '%')
                           OR LOWER(c.provincia.nombre) LIKE CONCAT('%', :q, '%'))
              AND (:tipo IS NULL OR c.tipo = :tipo)
            """)
    Page<Centro> buscarSinVerificarConFiltros(
            @Param("id")   Long      id,
            @Param("q")    String    q,
            @Param("tipo") TipoCentro tipo,
            Pageable pageable);

    // Búsqueda pública de centros verificados con filtros opcionales,
    // ordenada por valoración media y número de reseñas
    @Query(value = """
            SELECT c.* FROM centros c
            WHERE c.verificado = true
              AND (:filtrarNombre    = false OR unaccent(lower(c.nombre_comercial)) LIKE CONCAT('%', :nombre, '%'))
              AND (:filtrarLocalidad = false OR unaccent(lower(c.localidad))        LIKE CONCAT('%', :localidad, '%'))
              AND (:provinciaId IS NULL OR c.provincia_id = :provinciaId)
              AND (CAST(:tipo AS text) IS NULL OR c.tipo = CAST(:tipo AS text))
            ORDER BY (
                SELECT COALESCE(AVG(v.estrellas), 0)
                FROM valoraciones v
                JOIN formaciones f ON v.formacion_id = f.id_formacion
                WHERE f.centro_id = c.id_centro
            ) DESC, (
                SELECT COUNT(v.id_valoracion)
                FROM valoraciones v
                JOIN formaciones f ON v.formacion_id = f.id_formacion
                WHERE f.centro_id = c.id_centro
            ) DESC, c.nombre_comercial ASC
            """,
           countQuery = """
            SELECT COUNT(*) FROM centros c
            WHERE c.verificado = true
              AND (:filtrarNombre    = false OR unaccent(lower(c.nombre_comercial)) LIKE CONCAT('%', :nombre, '%'))
              AND (:filtrarLocalidad = false OR unaccent(lower(c.localidad))        LIKE CONCAT('%', :localidad, '%'))
              AND (:provinciaId IS NULL OR c.provincia_id = :provinciaId)
              AND (CAST(:tipo AS text) IS NULL OR c.tipo = CAST(:tipo AS text))
            """,
           nativeQuery = true)
    Page<Centro> findVerificadosOrderByValoracion(
            @Param("filtrarNombre")    boolean filtrarNombre,
            @Param("nombre")          String nombre,
            @Param("filtrarLocalidad") boolean filtrarLocalidad,
            @Param("localidad")       String localidad,
            @Param("provinciaId")     Long provinciaId,
            @Param("tipo")            TipoCentro tipo,
            Pageable pageable);

    @Query("SELECT c FROM Centro c JOIN c.gestores g WHERE g.id = :usuarioId")
    Optional<Centro> findByGestorId(@Param("usuarioId") Long usuarioId);
}
