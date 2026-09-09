package com.jesuscastillo.escuela.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Todas las respuestas de error salen en formato RFC 7807 (application/problem+json),
 * como manda CONTRIBUTING.md. Spring serializa ProblemDetail con ese content type.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        p.setTitle("Recurso no encontrado");
        p.setDetail(ex.getMessage());
        return p;
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        p.setTitle("Conflicto");
        p.setDetail(ex.getMessage());
        return p;
    }

    @ExceptionHandler(InvitacionNoVigenteException.class)
    ProblemDetail invitacionNoVigente(InvitacionNoVigenteException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.GONE);
        p.setTitle("Invitación no vigente");
        p.setDetail(ex.getMessage());
        return p;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        p.setTitle("Datos inválidos");
        p.setDetail(ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Petición inválida"));
        return p;
    }

    /**
     * Reglas de negocio que no se pueden expresar con anotaciones de validación,
     * como exigir contraseña únicamente cuando el método de activación es PASSWORD.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        p.setTitle("Datos inválidos");
        p.setDetail(ex.getMessage());
        return p;
    }
}
