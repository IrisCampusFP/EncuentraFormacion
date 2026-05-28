package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.ComunidadAutonoma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComunidadAutonomaRepository extends JpaRepository<ComunidadAutonoma, Long> {
    Optional<ComunidadAutonoma> findByNombreIgnoreCase(String nombre);
}
