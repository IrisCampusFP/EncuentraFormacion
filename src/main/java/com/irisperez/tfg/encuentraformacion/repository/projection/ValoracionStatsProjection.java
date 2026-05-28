package com.irisperez.tfg.encuentraformacion.repository.projection;

// Proyección de Spring Data: define los campos que devuelve la query de estadísticas
// (media y total de valoraciones por formación), sin necesidad de una entidad JPA
public interface ValoracionStatsProjection {
    Long getFormacionId();
    Double getMedia();
    Long getTotal();
}
