package org.ups.citasalud.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;
import org.ups.citasalud.infrastructure.web.generated.model.Cita;
import org.ups.citasalud.infrastructure.web.generated.model.ConfirmarCitaRequest;
import org.ups.citasalud.infrastructure.web.generated.model.ErrorResponse;
import org.ups.citasalud.infrastructure.web.generated.model.FranjaHoraria;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** BDD (Given-When-Then), funcional. Acceptance Scenario 2 de spec.md, SC-003/SC-004. */
class FranjaYaOcupadaTest extends FunctionalTestBase {

    @Test
    @DisplayName("Given una franja ya ocupada, When otro paciente intenta bloquearla, Then no aparece disponible y el bloqueo responde 409 FRANJA_NO_DISPONIBLE")
    void given_franjaOcupada_when_otroPacienteIntentaBloquear_then_409() {
        UUID medicoId = crearMedico();
        UUID franjaId = crearFranjaDisponible(medicoId, Instant.now().plus(24, ChronoUnit.HOURS));

        // Paciente A reserva la franja por completo.
        como(PACIENTE_A, PACIENTE_A_PASSWORD).postForEntity("/franjas/{franjaId}/bloqueos", null, BloqueoTemporal.class, franjaId);
        ResponseEntity<Cita> citaA = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/citas", new ConfirmarCitaRequest(franjaId), Cita.class);
        assertThat(citaA.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // La franja ya no debe aparecer en la lista de disponibles del médico.
        ResponseEntity<FranjaHoraria[]> franjas = como(PACIENTE_B, PACIENTE_B_PASSWORD)
                .getForEntity("/medicos/{medicoId}/franjas", FranjaHoraria[].class, medicoId);
        assertThat(List.of(franjas.getBody())).extracting(FranjaHoraria::getId).doesNotContain(franjaId);

        // Un segundo paciente no puede bloquearla.
        ResponseEntity<ErrorResponse> intentoDeB = como(PACIENTE_B, PACIENTE_B_PASSWORD)
                .postForEntity("/franjas/{franjaId}/bloqueos", null, ErrorResponse.class, franjaId);
        assertThat(intentoDeB.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(intentoDeB.getBody().getCode()).isEqualTo(ErrorResponse.CodeEnum.FRANJA_NO_DISPONIBLE);
    }
}
