package org.ups.citasalud.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** BDD (Given-When-Then) — Principio II de la constitution. FR-011/FR-016/FR-017. */
class CitaTest {

    @Test
    @DisplayName("Given datos válidos, When se crea una Cita nueva, Then su estadoNotificacion inicia en PENDIENTE (FR-011)")
    void given_datosValidos_when_nueva_then_estadoPendiente() {
        Cita cita = Cita.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        assertThat(cita.getEstadoNotificacion()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(cita.getIntentosNotificacion()).isZero();
    }

    @Test
    @DisplayName("Given una cita registrada, When se marca la notificación como enviada, Then su estado pasa a ENVIADA (FR-014)")
    void given_citaRegistrada_when_marcarNotificacionEnviada_then_estadoEnviada() {
        Cita cita = Cita.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        cita.marcarNotificacionEnviada();

        assertThat(cita.getEstadoNotificacion()).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    @Test
    @DisplayName("Given fallos de envío agotados, When se registra el intento fallido, Then el estado queda FALLIDA sin perder la cita (FR-017)")
    void given_envioFallido_when_registrarIntentoFallido_then_estadoFallidaYCitaIntacta() {
        Cita cita = Cita.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        cita.registrarIntentoFallido();

        assertThat(cita.getEstadoNotificacion()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(cita.getIntentosNotificacion()).isEqualTo(1);
        assertThat(cita.getId()).isNotNull();
    }
}
