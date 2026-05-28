package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.FormacionFavorita;
import com.irisperez.tfg.encuentraformacion.model.entity.FormacionFavoritaId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FormacionFavoritaRepository extends JpaRepository<FormacionFavorita, FormacionFavoritaId> {

    @Query("SELECT ff FROM FormacionFavorita ff WHERE ff.estudiante.id = :estId ORDER BY ff.fechaGuardado DESC")
    Page<FormacionFavorita> findByEstudianteId(@Param("estId") Long estId, Pageable pageable);

    @Query("SELECT COUNT(ff) > 0 FROM FormacionFavorita ff WHERE ff.estudiante.id = :estId AND ff.formacion.uuid = :uuid")
    boolean existsByEstudianteIdAndFormacionUuid(@Param("estId") Long estId, @Param("uuid") UUID uuid);

    @Modifying
    @Query("DELETE FROM FormacionFavorita ff WHERE ff.estudiante.id = :estId AND ff.formacion.uuid = :uuid")
    void deleteByEstudianteIdAndFormacionUuid(@Param("estId") Long estId, @Param("uuid") UUID uuid);
}
