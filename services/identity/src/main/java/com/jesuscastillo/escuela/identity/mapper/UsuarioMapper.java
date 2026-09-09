package com.jesuscastillo.escuela.identity.mapper;

import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Instant;

/**
 * Mapeo entre la entidad {@link Usuario} y los DTO del contrato.
 * <p>
 * La entidad nunca se expone directamente en la API: campos como {@code passwordHash}
 * o {@code schoolId} no deben salir del servicio.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UsuarioMapper {

    /**
     * @param usuario            entidad persistida
     * @param invitacionExpiraEn vencimiento de la invitación vigente, o {@code null}
     *                           si el usuario ya está activo
     */
    @Mapping(target = "invitacionExpiraEn", source = "invitacionExpiraEn")
    @Mapping(target = "id", source = "usuario.id")
    @Mapping(target = "nombre", source = "usuario.nombre")
    @Mapping(target = "correo", source = "usuario.correo")
    @Mapping(target = "rol", source = "usuario.rol")
    @Mapping(target = "estado", source = "usuario.estado")
    UsuarioResponse aResponse(Usuario usuario, Instant invitacionExpiraEn);

    default UsuarioResponse aResponse(Usuario usuario) {
        return aResponse(usuario, null);
    }
}
