package com.jesuscastillo.escuela.identity.controller;

import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest;
import com.jesuscastillo.escuela.identity.dto.InvitacionResponse;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.service.InvitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aceptación de invitaciones.
 * <p>
 * Endpoints públicos por diseño: quien llega con la liga del correo todavía no tiene
 * cuenta activa. La protección viene del token de un solo uso, no de la sesión.
 * Implementa la sección {@code /invitaciones} del contrato.
 */
@RestController
@RequestMapping("/invitaciones")
@RequiredArgsConstructor
@Tag(name = "invitaciones", description = "Activacion de cuentas con token de un solo uso")
public class InvitacionController {

    private final InvitacionService invitacionService;

    @Operation(summary = "Valida una invitacion sin consumirla")
    @ApiResponse(responseCode = "200", description = "Invitacion vigente")
    @ApiResponse(responseCode = "404", description = "No existe")
    @ApiResponse(responseCode = "410", description = "Expirada o ya usada")
    @GetMapping("/{token}")
    public InvitacionResponse validar(@PathVariable String token) {
        return invitacionService.validar(token);
    }

    @Operation(summary = "Activa la cuenta registrando la credencial elegida")
    @ApiResponse(responseCode = "200", description = "Cuenta ACTIVA")
    @ApiResponse(responseCode = "410", description = "Expirada o ya usada")
    @PostMapping("/{token}/aceptar")
    public UsuarioResponse aceptar(@PathVariable String token,
                                   @Valid @RequestBody AceptarInvitacionRequest request) {
        return invitacionService.aceptar(token, request);
    }
}
