package com.jesuscastillo.escuela.identity.service.impl;

import com.jesuscastillo.escuela.identity.config.EscuelaProperties;
import com.jesuscastillo.escuela.identity.dto.CrearUsuarioRequest;
import com.jesuscastillo.escuela.identity.dto.PaginaUsuarios;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.EstadoUsuario;
import com.jesuscastillo.escuela.identity.entity.Invitacion;
import com.jesuscastillo.escuela.identity.entity.Rol;
import com.jesuscastillo.escuela.identity.entity.TipoVinculo;
import com.jesuscastillo.escuela.identity.entity.Usuario;
import com.jesuscastillo.escuela.identity.entity.Vinculo;
import com.jesuscastillo.escuela.identity.event.UsuarioSuspendidoV1;
import com.jesuscastillo.escuela.identity.exception.ConflictoException;
import com.jesuscastillo.escuela.identity.exception.RecursoNoEncontradoException;
import com.jesuscastillo.escuela.identity.mapper.UsuarioMapper;
import com.jesuscastillo.escuela.identity.repository.UsuarioRepository;
import com.jesuscastillo.escuela.identity.service.InvitacionService;
import com.jesuscastillo.escuela.identity.service.OutboxService;
import com.jesuscastillo.escuela.identity.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementación del servicio de gestión de usuarios.
 * <p>
 * Flujo de alta:
 * 1. Verifica que el correo no exista (el correo es la identidad única del usuario).
 * 2. Crea el usuario en estado PENDIENTE, con los vínculos que apliquen según el rol.
 * 3. Delega en el servicio de invitaciones la emisión del token de un solo uso.
 * <p>
 * Flujo de suspensión:
 * 1. Cambia el estado a SUSPENDIDO (baja lógica: nunca se borra un usuario).
 * 2. Registra {@code usuario.suspendido.v1} en el outbox para que los demás servicios
 *    cierren conversaciones y revoquen accesos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final InvitacionService invitacionService;
    private final OutboxService outboxService;
    private final UsuarioMapper usuarioMapper;
    private final EscuelaProperties properties;

    @Override
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {

        String correo = request.correo().toLowerCase().trim();

        if (usuarioRepository.existsByCorreo(correo)) {
            log.warn("Alta rechazada: el correo ya esta registrado");
            throw new ConflictoException("Ya existe un usuario con ese correo");
        }

        Usuario usuario = Usuario.pendiente(
                properties.schoolId(), request.nombre().trim(), correo, request.rol());

        agregarVinculos(usuario, request);

        usuarioRepository.save(usuario);
        Invitacion invitacion = invitacionService.emitirPara(usuario);

        log.info("Usuario dado de alta id={} rol={}", usuario.getId(), usuario.getRol());

        return usuarioMapper.aResponse(usuario, invitacion.getExpiraEn());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaUsuarios listar(Rol rol, EstadoUsuario estado, Pageable pageable) {
        return PaginaUsuarios.de(
                usuarioRepository.buscar(rol, estado, pageable),
                usuarioMapper::aResponse);
    }

    @Override
    @Transactional
    public void suspender(UUID id) {

        Usuario usuario = localizar(id);

        if (usuario.getEstado() == EstadoUsuario.SUSPENDIDO) {
            log.info("Usuario id={} ya estaba suspendido, no se repite el evento", id);
            return;
        }

        usuario.suspender();

        outboxService.registrar(UsuarioSuspendidoV1.de(usuario));

        log.info("Usuario suspendido id={}", id);
    }

    @Override
    @Transactional
    public UsuarioResponse reinvitar(UUID id) {

        Usuario usuario = localizar(id);

        if (usuario.estaActivo()) {
            log.warn("Reinvitacion rechazada: usuario id={} ya esta activo", id);
            throw new ConflictoException("El usuario ya activo su cuenta");
        }

        Invitacion invitacion = invitacionService.emitirPara(usuario);
        log.info("Invitacion regenerada para usuario id={}", id);

        return usuarioMapper.aResponse(usuario, invitacion.getExpiraEn());
    }

    private Usuario localizar(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
    }

    /** Los vínculos con alumnos solo tienen sentido para un TUTOR. */
    private static void agregarVinculos(Usuario usuario, CrearUsuarioRequest request) {

        List<UUID> hijos = request.hijosIds();
        if (hijos == null || hijos.isEmpty()) {
            return;
        }

        if (request.rol() != Rol.TUTOR) {
            log.warn("Se ignoran {} vinculos: solo aplican al rol TUTOR", hijos.size());
            return;
        }

        hijos.forEach(hijoId -> usuario.getVinculos().add(new Vinculo(TipoVinculo.HIJO, hijoId)));
    }
}
