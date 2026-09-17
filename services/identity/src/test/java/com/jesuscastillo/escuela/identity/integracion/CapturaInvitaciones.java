package com.jesuscastillo.escuela.identity.integracion;

import com.jesuscastillo.escuela.identity.entity.Usuario;
import com.jesuscastillo.escuela.identity.service.NotificadorInvitaciones;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notificador de invitaciones para las pruebas: se queda con el token en claro en memoria.
 * <p>
 * El token en claro existe una sola vez, al emitir la invitación, y la implementación de
 * desarrollo lo entrega escribiéndolo en el log. Raspar el log ataría las pruebas al
 * formato de ese mensaje, que es presentación y puede cambiar cualquier día. Esta clase
 * es la segunda implementación que {@code NotificadorInvitaciones} anticipa: la prueba
 * depende de la interfaz y el canal se decide por configuración.
 */
class CapturaInvitaciones implements NotificadorInvitaciones {

    private final Map<String, String> tokensPorCorreo = new ConcurrentHashMap<>();

    @Override
    public void enviarInvitacion(Usuario usuario, String tokenEnClaro) {
        tokensPorCorreo.put(usuario.getCorreo(), tokenEnClaro);
    }

    /**
     * @throws AssertionError si nunca se emitió una invitación para ese correo; falla
     *                        aquí y no más adelante con un token nulo sin explicación
     */
    String tokenDe(String correo) {
        String token = tokensPorCorreo.get(correo);
        if (token == null) {
            throw new AssertionError("No se emitio ninguna invitacion para " + correo);
        }
        return token;
    }

    void limpiar() {
        tokensPorCorreo.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {

        /** Convive con LogNotificadorInvitaciones; @Primary decide cuál se inyecta. */
        @Bean
        @Primary
        CapturaInvitaciones capturaInvitaciones() {
            return new CapturaInvitaciones();
        }
    }
}
