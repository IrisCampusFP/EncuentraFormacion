package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Notificacion;
import com.irisperez.tfg.encuentraformacion.model.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

    Long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    Long countByUsuarioIdAndLeidaFalseAndTipoNot(Long usuarioId, TipoNotificacion tipo);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false")
    void marcarTodasLeidasPorUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false AND n.urlReferencia LIKE :urlPattern")
    void marcarLeidasPorUrlPatternYUsuario(@Param("usuarioId") Long usuarioId, @Param("urlPattern") String urlPattern);

    Optional<Notificacion> findFirstByUsuarioIdAndLeidaFalseAndUrlReferenciaOrderByFechaCreacionDesc(Long usuarioId, String urlReferencia);
}
