package com.jesuscastillo.escuela.identity.service;

import com.jesuscastillo.escuela.identity.entity.Usuario;

/**
 * Envío de la invitación al usuario recién dado de alta.
 * <p>
 * Funcionalidad:
 * Entregar al invitado la liga con su token de un solo uso. La implementación real
 * (correo/push) llega con el servicio {@code notifications} en la Fase 4; mientras
 * tanto se usa una implementación que deja la liga en el log.
 */
public interface NotificadorInvitaciones {

    /**
     * @param usuario     destinatario de la invitación
     * @param tokenEnClaro token de un solo uso; NO se persiste en ningún lado
     */
    void enviarInvitacion(Usuario usuario, String tokenEnClaro);
}
