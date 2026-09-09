package com.jesuscastillo.escuela.identity.repository;

import com.jesuscastillo.escuela.identity.entity.Invitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvitacionRepository extends JpaRepository<Invitacion, UUID> {

    Optional<Invitacion> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update Invitacion i set i.invalidada = true where i.usuario.id = :usuarioId and i.usadaEn is null")
    int invalidarPendientesDe(@Param("usuarioId") UUID usuarioId);
}
