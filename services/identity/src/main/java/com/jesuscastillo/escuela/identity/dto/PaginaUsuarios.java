package com.jesuscastillo.escuela.identity.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados — {@code PaginaUsuarios} del contrato.
 * <p>
 * Se expone una forma propia en lugar del {@code Page} de Spring Data para no atar
 * el contrato público a la serialización interna del framework.
 */
public record PaginaUsuarios(
        List<UsuarioResponse> contenido,
        int pagina,
        int totalPaginas,
        long totalElementos
) {

    public static <T> PaginaUsuarios de(Page<T> page, Function<T, UsuarioResponse> mapeo) {
        return new PaginaUsuarios(
                page.getContent().stream().map(mapeo).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }
}
