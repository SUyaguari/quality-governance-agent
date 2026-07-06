package org.ups.citasalud.domain.exception;

/** FR-004/FR-009/FR-013: la franja está ocupada, bloqueada por otro paciente, o el bloqueo venció. */
public class FranjaNoDisponibleException extends RuntimeException {
    public FranjaNoDisponibleException(String message) {
        super(message);
    }
}
