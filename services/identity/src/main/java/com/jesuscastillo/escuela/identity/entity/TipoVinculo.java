package com.jesuscastillo.escuela.identity.entity;

/**
 * Vínculo de un usuario con otra entidad del dominio:
 * un TUTOR se vincula con sus HIJOs; un MAESTRO con sus GRUPOs.
 */
public enum TipoVinculo {
    HIJO,
    GRUPO
}
