package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.TipoEstudios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoEstudiosRepository extends JpaRepository<TipoEstudios, Long> {
    Optional<TipoEstudios> findByNombreIgnoreCase(String nombre);
}
