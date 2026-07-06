package org.ups.citasalud.domain.port;

import org.ups.citasalud.domain.model.Cita;

/**
 * FR-014/FR-016: envío de la confirmación de la cita. El adapter que implemente este Port MUST
 * reintentar automáticamente ante fallos (research.md §7) y lanzar
 * {@link NotificacionFallidaException} solo si los reintentos se agotan sin éxito.
 */
public interface NotificacionCitaPort {

    void enviarConfirmacion(Cita cita, String nombreCompleto, String numeroWhatsApp);

    class NotificacionFallidaException extends RuntimeException {
        public NotificacionFallidaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
