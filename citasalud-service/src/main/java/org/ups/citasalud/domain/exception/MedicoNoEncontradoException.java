package org.ups.citasalud.domain.exception;

/** No existe un médico con el id solicitado (contrato: 404 MEDICO_NO_ENCONTRADO). */
public class MedicoNoEncontradoException extends RuntimeException {
    public MedicoNoEncontradoException(String message) {
        super(message);
    }
}
