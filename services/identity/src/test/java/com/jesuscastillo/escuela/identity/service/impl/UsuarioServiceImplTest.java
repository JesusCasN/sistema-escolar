package com.jesuscastillo.escuela.identity.service.impl;

import com.jesuscastillo.escuela.identity.config.EscuelaProperties;
import com.jesuscastillo.escuela.identity.dto.CrearUsuarioRequest;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.EstadoUsuario;
import com.jesuscastillo.escuela.identity.entity.Invitacion;
import com.jesuscastillo.escuela.identity.entity.Rol;
import com.jesuscastillo.escuela.identity.entity.TipoVinculo;
import com.jesuscastillo.escuela.identity.entity.Usuario;
import com.jesuscastillo.escuela.identity.exception.ConflictoException;
import com.jesuscastillo.escuela.identity.exception.RecursoNoEncontradoException;
import com.jesuscastillo.escuela.identity.mapper.UsuarioMapper;
import com.jesuscastillo.escuela.identity.repository.UsuarioRepository;
import com.jesuscastillo.escuela.identity.service.InvitacionService;
import com.jesuscastillo.escuela.identity.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.List;
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
class UsuarioServiceImplTest {

    private static final UUID SCHOOL_ID = UUID.fromString("5f3c1f9e-0000-4000-8000-000000000001");

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InvitacionService invitacionService;
    @Mock private OutboxService outboxService;
    @Mock private UsuarioMapper usuarioMapper;

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        EscuelaProperties properties = new EscuelaProperties(
                SCHOOL_ID,
                new EscuelaProperties.Invitacion(Duration.ofHours(72), "http://localhost:4200"));

        service = new UsuarioServiceImpl(
                usuarioRepository, invitacionService, outboxService, usuarioMapper, properties);

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(invitacionService.emitirPara(any(Usuario.class)))
                .thenAnswer(i -> Invitacion.para(i.getArgument(0), "hash-de-prueba"));
        when(usuarioMapper.aResponse(any(Usuario.class), any()))
                .thenAnswer(i -> {
                    Usuario u = i.getArgument(0);
                    return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(),
                            u.getRol(), u.getEstado(), i.getArgument(1));
                });
    }

    @Test
    void crear_correoNuevo_dejaUsuarioPendienteYEmiteInvitacion() {
        when(usuarioRepository.existsByCorreo("ana@escuela.mx")).thenReturn(false);

        UsuarioResponse response = service.crear(
                new CrearUsuarioRequest("Ana Ruiz", "ana@escuela.mx", Rol.MAESTRO, null));

        assertThat(response.estado()).isEqualTo(EstadoUsuario.PENDIENTE);
        assertThat(response.invitacionExpiraEn()).isNotNull();

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        assertThat(capturado.getValue().getSchoolId()).isEqualTo(SCHOOL_ID);
        verify(invitacionService).emitirPara(any(Usuario.class));
    }

    @Test
    void crear_correoConMayusculasYEspacios_seNormaliza() {
        when(usuarioRepository.existsByCorreo("ana@escuela.mx")).thenReturn(false);

        service.crear(new CrearUsuarioRequest("Ana Ruiz", "  ANA@Escuela.MX  ", Rol.MAESTRO, null));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        assertThat(capturado.getValue().getCorreo()).isEqualTo("ana@escuela.mx");
    }

    @Test
    void crear_correoDuplicado_lanzaConflictoYNoEmiteInvitacion() {
        when(usuarioRepository.existsByCorreo("ana@escuela.mx")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(
                new CrearUsuarioRequest("Ana Ruiz", "ana@escuela.mx", Rol.MAESTRO, null)))
                .isInstanceOf(ConflictoException.class);

        verify(usuarioRepository, never()).save(any());
        verify(invitacionService, never()).emitirPara(any());
    }

    @Test
    void crear_tutorConHijos_registraLosVinculos() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        UUID hijo = UUID.randomUUID();

        service.crear(new CrearUsuarioRequest("Luis Mora", "luis@correo.mx", Rol.TUTOR, List.of(hijo)));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        assertThat(capturado.getValue().getVinculos())
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.getTipo()).isEqualTo(TipoVinculo.HIJO);
                    assertThat(v.getRefId()).isEqualTo(hijo);
                });
    }

    @Test
    void crear_maestroConHijos_ignoraLosVinculos() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        service.crear(new CrearUsuarioRequest(
                "Ana Ruiz", "ana@escuela.mx", Rol.MAESTRO, List.of(UUID.randomUUID())));

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        assertThat(capturado.getValue().getVinculos()).isEmpty();
    }

    @Test
    void suspender_usuarioActivo_cambiaEstadoYRegistraEvento() {
        Usuario usuario = usuarioDePrueba();
        usuario.activar();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        service.suspender(usuario.getId());

        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        verify(outboxService).registrar(eq("usuario.suspendido"), anyInt(), eq(usuario.getId()), any());
    }

    @Test
    void suspender_usuarioYaSuspendido_noRepiteElEvento() {
        Usuario usuario = usuarioDePrueba();
        usuario.suspender();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        service.suspender(usuario.getId());

        verify(outboxService, never()).registrar(anyString(), anyInt(), any(), any());
    }

    @Test
    void suspender_usuarioInexistente_lanzaNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suspender(id))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void reinvitar_usuarioPendiente_emiteNuevaInvitacion() {
        Usuario usuario = usuarioDePrueba();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        UsuarioResponse response = service.reinvitar(usuario.getId());

        assertThat(response.invitacionExpiraEn()).isNotNull();
        verify(invitacionService).emitirPara(usuario);
    }

    @Test
    void reinvitar_usuarioYaActivo_lanzaConflicto() {
        Usuario usuario = usuarioDePrueba();
        usuario.activar();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.reinvitar(usuario.getId()))
                .isInstanceOf(ConflictoException.class);

        verify(invitacionService, never()).emitirPara(any());
    }

    private static Usuario usuarioDePrueba() {
        return Usuario.pendiente(SCHOOL_ID, "Ana Ruiz", "ana@escuela.mx", Rol.MAESTRO);
    }
}
