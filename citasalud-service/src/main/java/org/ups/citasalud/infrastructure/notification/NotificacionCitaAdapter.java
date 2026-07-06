package org.ups.citasalud.infrastructure.notification;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.port.NotificacionCitaPort;

/**
 * FR-014: envía la confirmación por WhatsApp al número del perfil del paciente, delegando el
 * envío HTTP (con reintentos, FR-016) a {@link WhatsAppHttpSender}.
 * FR-017: si los reintentos se agotan, traduce el fallo a {@link NotificacionFallidaException} y
 * el caller (ConfirmarCitaUseCase) NUNCA revierte el registro de la cita ya hecho.
 */
@Component
public class NotificacionCitaAdapter implements NotificacionCitaPort {

    private final WhatsAppHttpSender whatsAppHttpSender;

    public NotificacionCitaAdapter(WhatsAppHttpSender whatsAppHttpSender) {
        this.whatsAppHttpSender = whatsAppHttpSender;
    }

    @Override
    public void enviarConfirmacion(Cita cita, String nombreCompleto, String numeroWhatsApp) {
        try {
            whatsAppHttpSender.enviar(numeroWhatsApp, construirTexto(cita, nombreCompleto));
        } catch (RestClientException e) {
            throw new NotificacionFallidaException(
                    "No se pudo enviar la confirmación de la cita " + cita.getId() + " por WhatsApp tras los reintentos configurados",
                    e);
        }
    }

    private String construirTexto(Cita cita, String nombreCompleto) {
        return "Hola " + nombreCompleto + ", tu cita quedó confirmada para " + cita.getFechaHoraCita() + ".";
    }
}
