package com.jesuscastillo.escuela.identity.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 254)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado;

    /** Hash Argon2id; nulo si el usuario solo usa passkeys. */
    @Column(name = "password_hash")
    private String passwordHash;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "usuario_vinculo", joinColumns = @JoinColumn(name = "usuario_id"))
    private List<Vinculo> vinculos = new ArrayList<>();

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public static Usuario pendiente(UUID schoolId, String nombre, String correo, Rol rol) {
        Usuario u = new Usuario();
        u.id = UUID.randomUUID();
        u.schoolId = schoolId;
        u.nombre = nombre;
        u.correo = correo.toLowerCase().trim();
        u.rol = rol;
        u.estado = EstadoUsuario.PENDIENTE;
        u.creadoEn = Instant.now();
        u.actualizadoEn = u.creadoEn;
        return u;
    }

    public void activar() {
        this.estado = EstadoUsuario.ACTIVO;
        this.actualizadoEn = Instant.now();
    }

    public void suspender() {
        this.estado = EstadoUsuario.SUSPENDIDO;
        this.actualizadoEn = Instant.now();
    }

    public boolean estaActivo() {
        return this.estado == EstadoUsuario.ACTIVO;
    }
}
