package com.jesuscastillo.escuela.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Invitación de un solo uso. Solo se persiste el HASH (SHA-256) del token;
 * el token en claro viaja únicamente en el correo de invitación.
 */
@Entity
@Table(name = "invitacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitacion {

    /** Vigencia por defecto cuando no se configura otra (SPEC-001). */
    public static final Duration VIGENCIA_POR_DEFECTO = Duration.ofHours(72);

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "usada_en")
    private Instant usadaEn;

    @Column(name = "invalidada", nullable = false)
    private boolean invalidada;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    public static Invitacion para(Usuario usuario, String tokenHash) {
        return para(usuario, tokenHash, VIGENCIA_POR_DEFECTO);
    }

    public static Invitacion para(Usuario usuario, String tokenHash, Duration vigencia) {
        Invitacion i = new Invitacion();
        i.id = UUID.randomUUID();
        i.usuario = usuario;
        i.tokenHash = tokenHash;
        i.creadaEn = Instant.now();
        i.expiraEn = i.creadaEn.plus(vigencia == null ? VIGENCIA_POR_DEFECTO : vigencia);
        i.invalidada = false;
        return i;
    }

    public boolean vigente() {
        return !invalidada && usadaEn == null && Instant.now().isBefore(expiraEn);
    }

    public void marcarUsada() {
        this.usadaEn = Instant.now();
    }

    public void invalidar() {
        this.invalidada = true;
    }
}
