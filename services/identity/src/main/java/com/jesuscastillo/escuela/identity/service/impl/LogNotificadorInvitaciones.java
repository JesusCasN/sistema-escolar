package com.jesuscastillo.escuela.identity.service.impl;

import com.jesuscastillo.escuela.identity.config.EscuelaProperties;
import com.jesuscastillo.escuela.identity.entity.Usuario;
import com.jesuscastillo.escuela.identity.service.NotificadorInvitaciones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación de desarrollo del notificador: deja la liga de invitación en el log.
 * <p>
 * Flujo:
 * 1. Arma la liga con la URL base configurada y el token en claro.
 * 2. La escribe en el log para poder probar el alta de punta a punta sin correo.
 * <p>
 * Se reemplaza por la implementación real cuando exista el servicio
 * {@code notifications} (Fase 4).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogNotificadorInvitaciones implements NotificadorInvitaciones {

    private final EscuelaProperties properties;

    @Override
    public void enviarInvitacion(Usuario usuario, String tokenEnClaro) {
        log.info("""

                        =========== INVITACION (modo desarrollo) ===========
                        Para   : {} <{}>
                        Rol    : {}
                        Liga   : {}/invitacion/{}
                        ====================================================""",
                usuario.getNombre(), usuario.getCorreo(), usuario.getRol(),
                properties.invitacion().urlBase(), tokenEnClaro);
    }
}
