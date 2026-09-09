package com.jesuscastillo.escuela.identity.service;

import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest;
import com.jesuscastillo.escuela.identity.dto.InvitacionResponse;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.Invitacion;
import com.jesuscastillo.escuela.identity.entity.Usuario;

/**
 * Servicio de invitaciones de un solo uso (SPEC-001).
 * <p>
 * Funcionalidad:
 * Emitir invitaciones para usuarios recién dados de alta, validarlas cuando el
 * invitado abre la liga, y activarlas cuando define su credencial. El token en
 * claro solo existe en el momento de la emisión: en base de datos vive su hash.
 */
public interface InvitacionService {

    /**
     * Emite una invitación para un usuario, invalidando las anteriores que siguieran
     * pendientes, y dispara la notificación al invitado.
     *
     * @param usuario usuario destinatario
     * @return la invitación persistida (para conocer su vencimiento)
     */
    Invitacion emitirPara(Usuario usuario);

    /**
     * Valida una invitación sin consumirla, para pintar la pantalla de activación.
     *
     * @param token token en claro que viene en la liga
     * @throws com.jesuscastillo.escuela.identity.exception.RecursoNoEncontradoException
     *         si el token no corresponde a ninguna invitación
     * @throws com.jesuscastillo.escuela.identity.exception.InvitacionNoVigenteException
     *         si expiró, ya se usó o fue invalidada
     */
    InvitacionResponse validar(String token);

    /**
     * Consume la invitación y activa la cuenta con la credencial elegida.
     * Publica {@code usuario.activado.v1} a través del outbox, en la misma
     * transacción que el cambio de estado.
     *
     * @param token   token en claro
     * @param request método de activación y credencial cuando aplica
     * @return el usuario ya activo
     */
    UsuarioResponse aceptar(String token, AceptarInvitacionRequest request);
}
