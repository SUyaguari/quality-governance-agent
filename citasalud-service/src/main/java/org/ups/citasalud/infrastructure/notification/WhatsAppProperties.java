package org.ups.citasalud.infrastructure.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * research.md §7: número de reintentos e intervalo como parámetro configurable, no como un valor
 * de negocio fijo impuesto por el spec (ver spec.md § Assumptions).
 */
@Component
@ConfigurationProperties(prefix = "app.notificaciones.whatsapp")
public class WhatsAppProperties {

    private String baseUrl;
    private int reintentos = 3;
    private long backoffInicialMs = 500;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getReintentos() {
        return reintentos;
    }

    public void setReintentos(int reintentos) {
        this.reintentos = reintentos;
    }

    public long getBackoffInicialMs() {
        return backoffInicialMs;
    }

    public void setBackoffInicialMs(long backoffInicialMs) {
        this.backoffInicialMs = backoffInicialMs;
    }
}
