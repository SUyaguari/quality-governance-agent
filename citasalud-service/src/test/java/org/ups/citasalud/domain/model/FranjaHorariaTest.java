package org.ups.citasalud.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ups.citasalud.domain.exception.FranjaNoDisponibleException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BDD (Given-When-Then) — Principio II de la constitution. FR-002/FR-009/FR-010/FR-013. */
class FranjaHorariaTest {

    private final Instant ahora = Instant.parse("2026-08-01T10:00:00Z");
    private final UUID medicoId = UUID.randomUUID();
    private final UUID pacienteId = UUID.randomUUID();
    private final UUID otroPacienteId = UUID.randomUUID();

    @Nested
    @DisplayName("FR-002: franjas futuras")
    class Futura {

        @Test
        @DisplayName("Given una franja con fecha futura, When se pregunta si es futura, Then responde true")
        void given_fechaFutura_when_esFutura_then_true() {
            FranjaHoraria franja = FranjaHoraria.disponible(UUID.randomUUID(), medicoId, ahora.plus(Duration.ofHours(1)));

            assertThat(franja.esFutura(ahora)).isTrue();
        }

        @Test
        @DisplayName("Given una franja con fecha pasada, When se pregunta si es futura, Then responde false")
        void given_fechaPasada_when_esFutura_then_false() {
            FranjaHoraria franja = FranjaHoraria.disponible(UUID.randomUUID(), medicoId, ahora.minus(Duration.ofHours(1)));

            assertThat(franja.esFutura(ahora)).isFalse();
        }
    }

    @Nested
    @DisplayName("FR-009: bloqueo temporal")
    class Bloqueo {

        @Test
        @DisplayName("Given una franja disponible, When el paciente la bloquea, Then queda BLOQUEADA_TEMPORALMENTE a su favor")
        void given_franjaDisponible_when_bloquearPara_then_quedaBloqueada() {
            FranjaHoraria franja = FranjaHoraria.disponible(UUID.randomUUID(), medicoId, ahora.plus(Duration.ofDays(1)));

            franja.bloquearPara(pacienteId, ahora, Duration.ofMinutes(5));

            assertThat(franja.getEstado()).isEqualTo(EstadoFranja.BLOQUEADA_TEMPORALMENTE);
            assertThat(franja.getBloqueadaPorPacienteId()).isEqualTo(pacienteId);
            assertThat(franja.getBloqueadaHasta()).isEqualTo(ahora.plus(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("Given una franja ya OCUPADA, When se intenta bloquear, Then lanza FranjaNoDisponibleException")
        void given_franjaOcupada_when_bloquearPara_then_lanzaExcepcion() {
            FranjaHoraria franja = new FranjaHoraria(UUID.randomUUID(), medicoId, ahora.plus(Duration.ofDays(1)),
                    EstadoFranja.OCUPADA, null, null);

            assertThatThrownBy(() -> franja.bloquearPara(pacienteId, ahora, Duration.ofMinutes(5)))
                    .isInstanceOf(FranjaNoDisponibleException.class);
        }

        @Test
        @DisplayName("Given un bloqueo de otro paciente ya vencido, When se intenta bloquear, Then se permite (liberación perezosa)")
        void given_bloqueoVencido_when_bloquearPara_then_sePermite() {
            FranjaHoraria franja = new FranjaHoraria(UUID.randomUUID(), medicoId, ahora.plus(Duration.ofDays(1)),
                    EstadoFranja.BLOQUEADA_TEMPORALMENTE, otroPacienteId, ahora.minus(Duration.ofSeconds(1)));

            franja.bloquearPara(pacienteId, ahora, Duration.ofMinutes(5));

            assertThat(franja.getBloqueadaPorPacienteId()).isEqualTo(pacienteId);
        }
    }

    @Nested
    @DisplayName("FR-008/FR-013: confirmación")
    class Confirmacion {

        @Test
        @DisplayName("Given una franja bloqueada a favor del paciente y bloqueo vigente, When confirma, Then queda OCUPADA")
        void given_bloqueadaVigente_when_confirmarPara_then_ocupada() {
            FranjaHoraria franja = new FranjaHoraria(UUID.randomUUID(), medicoId, ahora.plus(Duration.ofDays(1)),
                    EstadoFranja.BLOQUEADA_TEMPORALMENTE, pacienteId, ahora.plus(Duration.ofMinutes(1)));

            franja.confirmarPara(pacienteId, ahora);

            assertThat(franja.getEstado()).isEqualTo(EstadoFranja.OCUPADA);
        }

        @Test
        @DisplayName("Given el bloqueo vencido, When el paciente intenta confirmar, Then lanza FranjaNoDisponibleException (FR-010/FR-013)")
        void given_bloqueoVencido_when_confirmarPara_then_lanzaExcepcion() {
            FranjaHoraria franja = new FranjaHoraria(UUID.randomUUID(), medicoId, ahora.plus(Duration.ofDays(1)),
                    EstadoFranja.BLOQUEADA_TEMPORALMENTE, pacienteId, ahora.minus(Duration.ofSeconds(1)));

            assertThatThrownBy(() -> franja.confirmarPara(pacienteId, ahora))
                    .isInstanceOf(FranjaNoDisponibleException.class);
            assertThat(franja.getEstado()).isEqualTo(EstadoFranja.DISPONIBLE);
        }

        @Test
        @DisplayName("Given bloqueada a favor de otro paciente, When un paciente distinto intenta confirmar, Then lanza FranjaNoDisponibleException")
        void given_bloqueadaPorOtro_when_confirmarPara_then_lanzaExcepcion() {
            FranjaHoraria franja = new FranjaHoraria(UUID.randomUUID(), medicoId, ahora.plus(Duration.ofDays(1)),
                    EstadoFranja.BLOQUEADA_TEMPORALMENTE, otroPacienteId, ahora.plus(Duration.ofMinutes(1)));

            assertThatThrownBy(() -> franja.confirmarPara(pacienteId, ahora))
                    .isInstanceOf(FranjaNoDisponibleException.class);
        }
    }
}
