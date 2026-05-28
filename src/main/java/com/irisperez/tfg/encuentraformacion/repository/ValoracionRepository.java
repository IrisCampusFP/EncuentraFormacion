package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Valoracion;
import com.irisperez.tfg.encuentraformacion.repository.projection.CentroValoracionStatsProjection;
import com.irisperez.tfg.encuentraformacion.repository.projection.ValoracionStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    // Calcula media y total de reseñas para varias formaciones a la vez (evita N+1 en listados)
    @Query("SELECT v.formacion.id AS formacionId, AVG(v.estrellas) AS media, COUNT(v) AS total " +
           "FROM Valoracion v WHERE v.formacion.id IN :ids GROUP BY v.formacion.id")
    List<ValoracionStatsProjection> findStatsByFormacionIds(@Param("ids") List<Long> ids);

    // Trae las reseñas de una formación con los datos del estudiante ya cargados, ordenadas por fecha
    @Query("SELECT v FROM Valoracion v " +
           "JOIN FETCH v.estudiante e " +
           "JOIN FETCH e.usuario u " +
           "WHERE v.formacion.id = :formacionId " +
           "ORDER BY v.fecha DESC")
    List<Valoracion> findByFormacionIdConEstudiante(@Param("formacionId") Long formacionId);

    // Valoración media de todas las formaciones de un centro
    @Query("SELECT AVG(v.estrellas) FROM Valoracion v WHERE v.formacion.centro.id = :centroId")
    Double findMediaByCentroId(@Param("centroId") Long centroId);

    // Número total de reseñas recibidas por un centro
    @Query("SELECT COUNT(v) FROM Valoracion v WHERE v.formacion.centro.id = :centroId")
    Long countByCentroId(@Param("centroId") Long centroId);

    // Igual que findStatsByFormacionIds pero agrupado por centro (para listados de centros)
    @Query("SELECT v.formacion.centro.id AS centroId, AVG(v.estrellas) AS media, COUNT(v) AS total " +
           "FROM Valoracion v WHERE v.formacion.centro.id IN :centroIds GROUP BY v.formacion.centro.id")
    List<CentroValoracionStatsProjection> findStatsByCentroIds(@Param("centroIds") List<Long> centroIds);

    boolean existsByEstudianteIdAndFormacionId(Long estId, Long formId);

    // Valoración propia del estudiante para una formación concreta (por UUID público)
    @Query("SELECT v FROM Valoracion v " +
           "JOIN FETCH v.estudiante e JOIN FETCH e.usuario u " +
           "JOIN v.formacion f " +
           "WHERE e.id = :estudianteId AND f.uuid = :uuid")
    java.util.Optional<Valoracion> findByEstudianteIdAndFormacionUuid(
            @Param("estudianteId") Long estudianteId,
            @Param("uuid") java.util.UUID uuid);
}
