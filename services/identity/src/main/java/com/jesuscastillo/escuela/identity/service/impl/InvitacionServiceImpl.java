package com.jesuscastillo.escuela.identity.service.impl;

import com.jesuscastillo.escuela.identity.config.EscuelaProperties;
import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest;
import com.jesuscastillo.escuela.identity.dto.InvitacionResponse;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.Invitacion;
import com.jesuscastillo.escuela.identity.entity.Usuario;
import com.jesuscastillo.escuela.identity.entity.Vinculo;
import com.jesuscastillo.escuela.identity.exception.InvitacionNoVigenteException;
import com.jesuscastillo.escuela.identity.exception.RecursoNoEncontradoException;
import com.jesuscastillo.escuela.identity.mapper.UsuarioMapper;
import com.jesuscastillo.escuela.identity.repository.InvitacionRepository;
import com.jesuscastillo.escuela.identity.service.InvitacionService;
import com.jesuscastillo.escuela.identity.service.InvitationTokenService;
import com.jesuscastillo.escuela.identity.service.NotificadorInvitaciones;
import com.jesuscastillo.escuela.identity.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de invitaciones.
 * <p>
 * Flujo de emisión:
 * 1. Invalida cualquier invitación pendiente del usuario (una vigente a la vez).
 * 2. Genera el token en claro y persiste únicamente su hash.
 * 3. Notifica al invitado con la liga; el token en claro no vuelve a existir.
 * <p>
 * Flujo de aceptación:
 * 1. Localiza la invitación por el hash del token recibido.
 * 2. Verifica vigencia: no invalidada, no usada y dentro del plazo.
 * 3. Registra la credencial (contraseña con Argon2id, o marca de passkey pendiente).
 * 4. Activa al usuario, marca la invitación como usada y registra
 *    {@code usuario.activado.v1} en el outbox — todo en la misma transacción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitacionServiceImpl implements InvitacionService {

    private static final String EVENTO_ACTIVADO = "usuario.activado";
    private static final int VERSION_EVENTO_ACTIVADO = 1;

    private final InvitacionRepository invitacionRepository;
    private final InvitationTokenService tokenService;
    private final NotificadorInvitaciones notificador;
    private final OutboxService outboxService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final EscuelaProperties properties;

    @Override
    @Transactional
    public Invitacion emitirPara(Usuario usuario) {

        int invalidadas = invitacionRepository.invalidarPendientesDe(usuario.getId());
        if (invalidadas > 0) {
            log.info("Invitacion: se invalidaron {} invitaciones previas de usuario={}",
                    invalidadas, usuario.getId());
        }

        String tokenEnClaro = tokenService.nuevoToken();
        Invitacion invitacion = invitacionRepository.save(
                Invitacion.para(usuario, tokenService.hash(tokenEnClaro),
                        properties.invitacion().vigencia()));

        notificador.enviarInvitacion(usuario, tokenEnClaro);

        log.info("Invitacion emitida para usuario={} rol={} expira={}",
                usuario.getId(), usuario.getRol(), invitacion.getExpiraEn());

        return invitacion;
    }

    @Override
    @Transactional(readOnly = true)
    public InvitacionResponse validar(String token) {

        Invitacion invitacion = localizar(token);
        Usuario usuario = invitacion.getUsuario();

        // Endpoint publico: solo se devuelve lo necesario para pintar la pantalla.
        return new InvitacionResponse(
                primerNombre(usuario.getNombre()),
                usuario.getRol(),
                invitacion.getExpiraEn());
    }

    @Override
    @Transactional
    public UsuarioResponse aceptar(String token, AceptarInvitacionRequest request) {

        Invitacion invitacion = localizar(token);
        Usuario usuario = invitacion.getUsuario();

        if (request.requierePassword()) {
            if (request.password() == null || request.password().isBlank()) {
                throw new IllegalArgumentException("La contrasena es obligatoria con el metodo PASSWORD");
            }
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        } else {
            // El ceremonial WebAuthn se completa contra Spring Security en el primer
            // acceso; aqui solo se deja la cuenta lista sin credencial de contrasena.
            usuario.setPasswordHash(null);
        }

        usuario.activar();
        invitacion.marcarUsada();

        outboxService.registrar(EVENTO_ACTIVADO, VERSION_EVENTO_ACTIVADO, usuario.getId(),
                Map.of(
                        "userId", usuario.getId(),
                        "schoolId", usuario.getSchoolId(),
                        "rol", usuario.getRol(),
                        "nombre", usuario.getNombre(),
                        "vinculos", vinculosComoMapa(usuario.getVinculos())));

        log.info("Cuenta activada usuario={} rol={} metodo={}",
                usuario.getId(), usuario.getRol(), request.metodo());

        return usuarioMapper.aResponse(usuario);
    }

    /**
     * Busca la invitación por el hash del token y valida su vigencia.
     *
     * @throws RecursoNoEncontradoException  si el token no existe
     * @throws InvitacionNoVigenteException  si expiró, ya se usó o fue invalidada
     */
    private Invitacion localizar(String token) {

        Invitacion invitacion = invitacionRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> {
                    log.warn("Invitacion: token inexistente");
                    return new RecursoNoEncontradoException("La invitacion no existe");
                });

        if (!invitacion.vigente()) {
            log.warn("Invitacion no vigente id={} usada={} invalidada={} expira={}",
                    invitacion.getId(), invitacion.getUsadaEn(),
                    invitacion.isInvalidada(), invitacion.getExpiraEn());
            throw new InvitacionNoVigenteException(
                    "La invitacion expiro o ya fue utilizada. Solicita una nueva a la direccion de la escuela");
        }

        return invitacion;
    }

    private static List<Map<String, Object>> vinculosComoMapa(List<Vinculo> vinculos) {
        return vinculos.stream()
                .map(v -> Map.<String, Object>of("tipo", v.getTipo(), "id", v.getRefId()))
                .toList();
    }

    /** El endpoint publico de validacion no revela el nombre completo. */
    private static String primerNombre(String nombreCompleto) {
        int espacio = nombreCompleto.indexOf(' ');
        return espacio > 0 ? nombreCompleto.substring(0, espacio) : nombreCompleto;
    }
}
