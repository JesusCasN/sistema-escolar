package com.jesuscastillo.escuela.identity.controller;

import com.jesuscastillo.escuela.identity.dto.CrearUsuarioRequest;
import com.jesuscastillo.escuela.identity.dto.PaginaUsuarios;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.EstadoUsuario;
import com.jesuscastillo.escuela.identity.entity.Rol;
import com.jesuscastillo.escuela.identity.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Gestión de usuarios por parte de dirección.
 * <p>
 * Implementa la sección {@code /admin/usuarios} del contrato
 * {@code contracts/openapi/identity.yaml}.
 */
@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
@Tag(name = "admin", description = "Gestion de usuarios (solo DIRECCION)")
public class AdminUsuarioController {

    /** Tope de tamaño de página declarado en el contrato. */
    private static final int TAMANO_PAGINA_MAXIMO = 100;

    private final UsuarioService usuarioService;

    @Operation(summary = "Alta de usuario (genera invitacion de un solo uso, TTL 72 h)")
    @ApiResponse(responseCode = "201", description = "Usuario creado en estado PENDIENTE")
    @ApiResponse(responseCode = "409", description = "El correo ya esta registrado")
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @Operation(summary = "Lista paginada de usuarios")
    @GetMapping
    public PaginaUsuarios listar(
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) EstadoUsuario estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, TAMANO_PAGINA_MAXIMO),
                Sort.by(Sort.Direction.DESC, "creadoEn"));

        return usuarioService.listar(rol, estado, pageable);
    }

    @Operation(summary = "Baja logica: deja al usuario en estado SUSPENDIDO")
    @ApiResponse(responseCode = "204", description = "Suspendido")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> suspender(@PathVariable UUID id) {
        usuarioService.suspender(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Regenera la invitacion e invalida la anterior")
    @ApiResponse(responseCode = "409", description = "El usuario ya esta ACTIVO")
    @PostMapping("/{id}/reinvitar")
    public UsuarioResponse reinvitar(@PathVariable UUID id) {
        return usuarioService.reinvitar(id);
    }
}
