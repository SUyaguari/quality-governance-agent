package org.ups.citasalud.infrastructure.notification;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * FR-016: envío HTTP al proveedor de WhatsApp con reintentos automáticos. Es un bean separado de
 * {@link NotificacionCitaAdapter} a propósito: {@code @Retryable} solo funciona a través del
 * proxy AOP de Spring cuando el método se invoca desde OTRO bean (una auto-invocación dentro de
 * la misma clase no pasa por el proxy y el reintento nunca se activaría).
 */
@Component
public class WhatsAppHttpSender {

    private final RestClient restClient;
    private final WhatsAppProperties properties;

    public WhatsAppHttpSender(RestClient.Builder restClientBuilder, WhatsAppProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Retryable(
            retryFor = RestClientException.class,
            maxAttemptsExpression = "#{@whatsAppProperties.reintentos}",
            backoff = @Backoff(delayExpression = "#{@whatsAppProperties.backoffInicialMs}")
    )
    public void enviar(String numeroWhatsApp, String texto) {
        restClient.post()
                .uri(properties.getBaseUrl())
                .body(new MensajeWhatsApp(numeroWhatsApp, texto))
                .retrieve()
                .toBodilessEntity();
    }

    private record MensajeWhatsApp(String to, String mensaje) {
    }
}
