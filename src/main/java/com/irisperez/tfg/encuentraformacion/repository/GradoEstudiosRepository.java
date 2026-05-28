package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.GradoEstudios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradoEstudiosRepository extends JpaRepository<GradoEstudios, Long> {
    Optional<GradoEstudios> findByNombre(String nombre);
    Optional<GradoEstudios> findByNombreIgnoreCase(String nombre);
}
