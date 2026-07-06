package org.ups.citasalud.domain.exception;

/** FR-018: el paciente ya tiene el máximo de 2 citas activas permitidas. */
public class LimiteCitasActivasExcedidoException extends RuntimeException {
    public LimiteCitasActivasExcedidoException(String message) {
        super(message);
    }
}
