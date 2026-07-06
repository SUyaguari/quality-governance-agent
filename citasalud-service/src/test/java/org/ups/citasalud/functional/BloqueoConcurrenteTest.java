package org.ups.citasalud.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;
import org.ups.citasalud.infrastructure.web.generated.model.ErrorResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** BDD (Given-When-Then), funcional. Acceptance Scenario 3 de spec.md, SC-003. */
class BloqueoConcurrenteTest extends FunctionalTestBase {

    @Test
    @DisplayName("Given el paciente A bloqueó una franja, When el paciente B intenta bloquear la misma franja mientras el bloqueo sigue vigente, Then se le muestra como no disponible")
    void given_franjaBloqueadaPorA_when_bIntentaBloquearla_then_noDisponibleParaB() {
        UUID medicoId = crearMedico();
        UUID franjaId = crearFranjaDisponible(medicoId, Instant.now().plus(24, ChronoUnit.HOURS));

        ResponseEntity<BloqueoTemporal> bloqueoA = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/franjas/{franjaId}/bloqueos", null, BloqueoTemporal.class, franjaId);
        assertThat(bloqueoA.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ErrorResponse> intentoDeB = como(PACIENTE_B, PACIENTE_B_PASSWORD)
                .postForEntity("/franjas/{franjaId}/bloqueos", null, ErrorResponse.class, franjaId);
        assertThat(intentoDeB.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(intentoDeB.getBody().getCode()).isEqualTo(ErrorResponse.CodeEnum.FRANJA_NO_DISPONIBLE);
    }
}
