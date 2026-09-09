package com.jesuscastillo.escuela.identity.repository;

import com.jesuscastillo.escuela.identity.entity.EstadoUsuario;
import com.jesuscastillo.escuela.identity.entity.Rol;
import com.jesuscastillo.escuela.identity.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    @Query("""
            select u from Usuario u
            where (:rol is null or u.rol = :rol)
              and (:estado is null or u.estado = :estado)
            """)
    Page<Usuario> buscar(@Param("rol") Rol rol,
                         @Param("estado") EstadoUsuario estado,
                         Pageable pageable);
}
