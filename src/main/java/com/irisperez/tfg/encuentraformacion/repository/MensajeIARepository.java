package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.MensajeIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeIARepository extends JpaRepository<MensajeIA, Long> {

    // Últimos 20 mensajes de una sesión (ventana deslizante para el LLM)
    List<MensajeIA> findTop20BySesionIdOrderByFechaEnvioAsc(Long sesionId);

    // Historial completo
    List<MensajeIA> findBySesionIdOrderByFechaEnvioAsc(Long sesionId);

    long countBySesionId(Long sesionId);
}
