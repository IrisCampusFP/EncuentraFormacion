package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.SesionIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SesionIARepository extends JpaRepository<SesionIA, Long> {

    List<SesionIA> findByEstudianteIdOrderByUltimaActividadDesc(Long estudianteId);

    Optional<SesionIA> findByIdAndEstudianteId(Long id, Long estudianteId);

    long countByEstudianteId(Long estudianteId);

    void deleteAllByEstudianteId(Long estudianteId);
}
