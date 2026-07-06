package org.ups.citasalud.domain.exception;

/** No existe una franja horaria con el id solicitado (contrato: 404 FRANJA_NO_ENCONTRADA). */
public class FranjaNoEncontradaException extends RuntimeException {
    public FranjaNoEncontradaException(String message) {
        super(message);
    }
}
