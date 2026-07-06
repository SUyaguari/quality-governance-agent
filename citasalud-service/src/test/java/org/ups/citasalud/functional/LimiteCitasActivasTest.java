package org.ups.citasalud.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;
import org.ups.citasalud.infrastructure.web.generated.model.Cita;
import org.ups.citasalud.infrastructure.web.generated.model.ConfirmarCitaRequest;
import org.ups.citasalud.infrastructure.web.generated.model.ErrorResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** BDD (Given-When-Then), funcional. Acceptance Scenario 6 de spec.md, FR-018/SC-008. */
class LimiteCitasActivasTest extends FunctionalTestBase {

    private UUID reservarCitaParaPacienteA(UUID medicoId, long horasEnElFuturo) {
        UUID franjaId = crearFranjaDisponible(medicoId, Instant.now().plus(horasEnElFuturo, ChronoUnit.HOURS));
        como(PACIENTE_A, PACIENTE_A_PASSWORD).postForEntity("/franjas/{franjaId}/bloqueos", null, BloqueoTemporal.class, franjaId);
        ResponseEntity<Cita> cita = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/citas", new ConfirmarCitaRequest(franjaId), Cita.class);
        assertThat(cita.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return franjaId;
    }

    @Test
    @DisplayName("Given un paciente con 2 citas activas, When intenta confirmar una tercera, Then el sistema la rechaza por límite de citas activas")
    void given_pacienteCon2CitasActivas_when_confirmaUnaTercera_then_rechazadaPorLimite() {
        UUID medicoId = crearMedico();
        reservarCitaParaPacienteA(medicoId, 24);
        reservarCitaParaPacienteA(medicoId, 48);

        UUID terceraFranjaId = crearFranjaDisponible(medicoId, Instant.now().plus(72, ChronoUnit.HOURS));
        como(PACIENTE_A, PACIENTE_A_PASSWORD).postForEntity("/franjas/{franjaId}/bloqueos", null, BloqueoTemporal.class, terceraFranjaId);

        ResponseEntity<ErrorResponse> intentoTercero = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/citas", new ConfirmarCitaRequest(terceraFranjaId), ErrorResponse.class);
        assertThat(intentoTercero.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(intentoTercero.getBody().getCode()).isEqualTo(ErrorResponse.CodeEnum.LIMITE_CITAS_ACTIVAS_EXCEDIDO);
    }
}
