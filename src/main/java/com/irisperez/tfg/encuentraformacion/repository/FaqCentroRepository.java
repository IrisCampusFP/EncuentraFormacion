package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.FaqCentro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqCentroRepository extends JpaRepository<FaqCentro, Long> {

    List<FaqCentro> findByCentroIdOrderByOrdenAsc(Long centroId);

    java.util.Optional<FaqCentro> findByIdAndCentroId(Long id, Long centroId);
}
