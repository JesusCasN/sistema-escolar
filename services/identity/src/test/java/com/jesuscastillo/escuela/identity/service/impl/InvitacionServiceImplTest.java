package com.jesuscastillo.escuela.identity.service.impl;

import com.jesuscastillo.escuela.identity.config.EscuelaProperties;
import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest;
import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest.MetodoActivacion;
import com.jesuscastillo.escuela.identity.dto.InvitacionResponse;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.EstadoUsuario;
import com.jesuscastillo.escuela.identity.entity.Invitacion;
import com.jesuscastillo.escuela.identity.entity.Rol;
import com.jesuscastillo.escuela.identity.entity.Usuario;
import com.jesuscastillo.escuela.identity.exception.InvitacionNoVigenteException;
import com.jesuscastillo.escuela.identity.exception.RecursoNoEncontradoException;
import com.jesuscastillo.escuela.identity.mapper.UsuarioMapper;
import com.jesuscastillo.escuela.identity.repository.InvitacionRepository;
import com.jesuscastillo.escuela.identity.service.InvitationTokenService;
import com.jesuscastillo.escuela.identity.service.NotificadorInvitaciones;
import com.jesuscastillo.escuela.identity.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitacionServiceImplTest {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final String TOKEN = "token-en-claro";
    private static final String HASH = "hash-del-token";

    @Mock private InvitacionRepository invitacionRepository;
    @Mock private InvitationTokenService tokenService;
    @Mock private NotificadorInvitaciones notificador;
    @Mock private OutboxService outboxService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UsuarioMapper usuarioMapper;

    private InvitacionServiceImpl service;

    @BeforeEach
    void setUp() {
        EscuelaProperties properties = new EscuelaProperties(
                SCHOOL_ID,
                new EscuelaProperties.Invitacion(Duration.ofHours(72), "http://localhost:4200"));

        service = new InvitacionServiceImpl(
                invitacionRepository, tokenService, notificador,
                outboxService, passwordEncoder, usuarioMapper, properties);

        when(tokenService.hash(TOKEN)).thenReturn(HASH);
        when(tokenService.nuevoToken()).thenReturn(TOKEN);
        when(invitacionRepository.save(any(Invitacion.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("$argon2id$hash-simulado");
        when(usuarioMapper.aResponse(any(Usuario.class)))
                .thenAnswer(i -> {
                    Usuario u = i.getArgument(0);
                    return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(),
                            u.getRol(), u.getEstado(), null);
                });
    }

    @Test
    void emitirPara_invalidaAnterioresYNotificaConTokenEnClaro() {
        Usuario usuario = usuarioDePrueba();
        when(invitacionRepository.invalidarPendientesDe(usuario.getId())).thenReturn(1);

        Invitacion invitacion = service.emitirPara(usuario);

        assertThat(invitacion.getTokenHash()).isEqualTo(HASH);
        assertThat(invitacion.vigente()).isTrue();
        verify(invitacionRepository).invalidarPendientesDe(usuario.getId());
        verify(notificador).enviarInvitacion(usuario, TOKEN);
    }

    @Test
    void validar_invitacionVigente_devuelveSoloPrimerNombreYRol() {
        Invitacion invitacion = Invitacion.para(usuarioDePrueba(), HASH);
        when(invitacionRepository.findByTokenHash(HASH)).thenReturn(Optional.of(invitacion));

        InvitacionResponse response = service.validar(TOKEN);

        // El endpoint es publico: no revela el nombre completo ni el correo.
        assertThat(response.nombre()).isEqualTo("Ana");
        assertThat(response.rol()).isEqualTo(Rol.MAESTRO);
        assertThat(response.expiraEn()).isEqualTo(invitacion.getExpiraEn());
    }

    @Test
    void validar_tokenInexistente_lanzaNoEncontrado() {
        when(invitacionRepository.findByTokenHash(HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validar(TOKEN))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void validar_invitacionYaUsada_lanzaNoVigente() {
        Invitacion invitacion = Invitacion.para(usuarioDePrueba(), HASH);
        invitacion.marcarUsada();
        when(invitacionRepository.findByTokenHash(HASH)).thenReturn(Optional.of(invitacion));

        assertThatThrownBy(() -> service.validar(TOKEN))
                .isInstanceOf(InvitacionNoVigenteException.class);
    }

    @Test
    void validar_invitacionInvalidada_lanzaNoVigente() {
        Invitacion invitacion = Invitacion.para(usuarioDePrueba(), HASH);
        invitacion.invalidar();
        when(invitacionRepository.findByTokenHash(HASH)).thenReturn(Optional.of(invitacion));

        assertThatThrownBy(() -> service.validar(TOKEN))
                .isInstanceOf(InvitacionNoVigenteException.class);
    }

    @Test
    void aceptar_conPassword_guardaHashActivaYRegistraEvento() {
        Usuario usuario = usuarioDePrueba();
        Invitacion invitacion = Invitacion.para(usuario, HASH);
        when(invitacionRepository.findByTokenHash(HASH)).thenReturn(Optional.of(invitacion));

        UsuarioResponse response = service.aceptar(TOKEN,
                new AceptarInvitacionRequest(MetodoActivacion.PASSWORD, "contrasena-larga-123"));

        assertThat(response.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.getPasswordHash()).startsWith("$argon2id$");
        assertThat(invitacion.vigente()).isFalse();
        verify(outboxService).registrar(eq("usuario.activado"), anyInt(), eq(usuario.getId()), any());
    }

    @Test
    void aceptar_conPasskey_noGuardaPasswordPeroActiva() {
        Usuario usuario = usuarioDePrueba();
        Invitacion invitacion = Invitacion.para(usuario, HASH);
        when(invitacionRepository.findByTokenHash(HASH)).thenReturn(Optional.of(invitacion));

        service.aceptar(TOKEN, new AceptarInvitacionRequest(MetodoActivacion.PASSKEY, null));

        assertThat(usuario.estaActivo()).isTrue();
        assertThat(usuario.getPasswordHash()).isNull();
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void aceptar_dosVeces_laSegundaFallaPorqueElTokenEsDeUnSoloUso() {
        Usuario usuario = usuarioDePrueba();
        Invitacion invitacion = Invitacion.para(usuario, HASH);
        when(invitacionRepository.findByTokenHash(HASH)).thenReturn(Optional.of(invitacion));

        service.aceptar(TOKEN, new AceptarInvitacionRequest(MetodoActivacion.PASSKEY, null));

        assertThatThrownBy(() -> service.aceptar(TOKEN,
                new AceptarInvitacionRequest(MetodoActivacion.PASSKEY, null)))
                .isInstanceOf(InvitacionNoVigenteException.class);
    }

    private static Usuario usuarioDePrueba() {
        return Usuario.pendiente(SCHOOL_ID, "Ana Ruiz", "ana@escuela.mx", Rol.MAESTRO);
    }
}
