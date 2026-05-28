package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Estudiante;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Optional<Estudiante> findByUsuarioId(Long usuarioId);

    @Query("SELECT ff.formacion.nombre FROM FormacionFavorita ff WHERE ff.estudiante.id = :estId ORDER BY ff.fechaGuardado DESC")
    List<String> findNombresFavoritosTop5(@Param("estId") Long estId, Pageable pageable);
}
