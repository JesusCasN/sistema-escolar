package com.jesuscastillo.escuela.identity.service;

import com.jesuscastillo.escuela.identity.dto.CrearUsuarioRequest;
import com.jesuscastillo.escuela.identity.dto.PaginaUsuarios;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.EstadoUsuario;
import com.jesuscastillo.escuela.identity.entity.Rol;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Servicio de gestión de usuarios, operado por dirección (SPEC-001).
 * <p>
 * Funcionalidad:
 * Dar de alta usuarios (que nacen en estado PENDIENTE con una invitación de un solo
 * uso), consultarlos, suspenderlos y regenerar invitaciones vencidas. No existe
 * auto-registro: la escuela controla quién entra al sistema.
 */
public interface UsuarioService {

    /**
     * Da de alta un usuario en estado PENDIENTE y emite su invitación.
     *
     * @param request datos del alta
     * @return el usuario creado, con el vencimiento de su invitación
     * @throws com.jesuscastillo.escuela.identity.exception.ConflictoException
     *         si el correo ya está registrado
     */
    UsuarioResponse crear(CrearUsuarioRequest request);

    /**
     * Lista usuarios con filtros opcionales.
     *
     * @param rol      filtro por rol, o {@code null} para todos
     * @param estado   filtro por estado, o {@code null} para todos
     * @param pageable paginación solicitada
     */
    PaginaUsuarios listar(Rol rol, EstadoUsuario estado, Pageable pageable);

    /**
     * Baja lógica: deja al usuario en estado SUSPENDIDO y publica el evento
     * {@code usuario.suspendido.v1} para que los demás servicios reaccionen.
     *
     * @param id identificador del usuario
     */
    void suspender(UUID id);

    /**
     * Regenera la invitación de un usuario que aún no activa su cuenta.
     * Invalida cualquier invitación anterior.
     *
     * @param id identificador del usuario
     * @throws com.jesuscastillo.escuela.identity.exception.ConflictoException
     *         si el usuario ya está ACTIVO
     */
    UsuarioResponse reinvitar(UUID id);
}
