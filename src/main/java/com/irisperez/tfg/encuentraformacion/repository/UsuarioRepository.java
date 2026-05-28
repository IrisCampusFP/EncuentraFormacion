package com.irisperez.tfg.encuentraformacion.repository;

import com.irisperez.tfg.encuentraformacion.model.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByActivoTrue();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByDni(String dni);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.centrosGestionados WHERE u.id = :id")
    Optional<Usuario> findByIdWithCentros(@Param("id") Long id);

    @Query(value = """
            SELECT u FROM Usuario u
            WHERE (:id IS NULL OR u.id = :id)
              AND (:q = '' OR LOWER(u.username) LIKE CONCAT('%', :q, '%')
                           OR LOWER(u.email)    LIKE CONCAT('%', :q, '%')
                           OR LOWER(u.nombre)   LIKE CONCAT('%', :q, '%')
                           OR (u.apellidos IS NOT NULL AND LOWER(u.apellidos) LIKE CONCAT('%', :q, '%')))
              AND (:activo IS NULL OR u.activo = :activo)
              AND (:rolId IS NULL OR EXISTS (SELECT r FROM u.roles r WHERE r.id = :rolId))
            """,
           countQuery = """
            SELECT COUNT(u) FROM Usuario u
            WHERE (:id IS NULL OR u.id = :id)
              AND (:q = '' OR LOWER(u.username) LIKE CONCAT('%', :q, '%')
                           OR LOWER(u.email)    LIKE CONCAT('%', :q, '%')
                           OR LOWER(u.nombre)   LIKE CONCAT('%', :q, '%')
                           OR (u.apellidos IS NOT NULL AND LOWER(u.apellidos) LIKE CONCAT('%', :q, '%')))
              AND (:activo IS NULL OR u.activo = :activo)
              AND (:rolId IS NULL OR EXISTS (SELECT r FROM u.roles r WHERE r.id = :rolId))
            """)
    Page<Usuario> buscarConFiltros(
            @Param("id")     Long    id,
            @Param("q")      String  q,
            @Param("activo") Boolean activo,
            @Param("rolId")  Long    rolId,
            Pageable pageable);
}
