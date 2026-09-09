package com.jesuscastillo.escuela.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class Vinculo {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVinculo tipo;

    @Column(name = "ref_id", nullable = false)
    private UUID refId;
}
