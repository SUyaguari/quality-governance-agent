package org.ups.citasalud.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.ups.citasalud.domain.exception.FranjaNoDisponibleException;
import org.ups.citasalud.domain.exception.FranjaNoEncontradaException;
import org.ups.citasalud.domain.exception.LimiteCitasActivasExcedidoException;
import org.ups.citasalud.domain.exception.MedicoNoEncontradoException;
import org.ups.citasalud.infrastructure.web.generated.model.ErrorResponse;

/** Traduce las excepciones de dominio a las respuestas de error del contrato (citasalud-api.yaml). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MedicoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarMedicoNoEncontrado(MedicoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorResponse.CodeEnum.MEDICO_NO_ENCONTRADO, ex.getMessage()));
    }

    @ExceptionHandler(FranjaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejarFranjaNoEncontrada(FranjaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorResponse.CodeEnum.FRANJA_NO_ENCONTRADA, ex.getMessage()));
    }

    @ExceptionHandler(FranjaNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarFranjaNoDisponible(FranjaNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorResponse.CodeEnum.FRANJA_NO_DISPONIBLE, ex.getMessage()));
    }

    @ExceptionHandler(LimiteCitasActivasExcedidoException.class)
    public ResponseEntity<ErrorResponse> manejarLimiteCitasActivasExcedido(LimiteCitasActivasExcedidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorResponse.CodeEnum.LIMITE_CITAS_ACTIVAS_EXCEDIDO, ex.getMessage()));
    }
}
