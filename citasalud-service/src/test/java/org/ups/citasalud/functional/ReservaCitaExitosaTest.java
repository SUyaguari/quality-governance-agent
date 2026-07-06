package org.ups.citasalud.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;
import org.ups.citasalud.infrastructure.web.generated.model.Cita;
import org.ups.citasalud.infrastructure.web.generated.model.ConfirmarCitaRequest;
import org.ups.citasalud.infrastructure.web.generated.model.Medico;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BDD (Given-When-Then), funcional end-to-end. Acceptance Scenario 1 de spec.md, SC-001/SC-005.
 */
class ReservaCitaExitosaTest extends FunctionalTestBase {

    @Test
    @DisplayName("Given un paciente autenticado, When elige médico/franja y confirma, Then la cita queda registrada y se intenta notificar por WhatsApp")
    void given_pacienteAutenticado_when_eligeYConfirma_then_citaRegistrada() {
        UUID medicoId = crearMedico();
        UUID franjaId = crearFranjaDisponible(medicoId, Instant.now().plus(24, ChronoUnit.HOURS));

        // FR-001: el paciente primero consulta los médicos disponibles.
        ResponseEntity<Medico[]> medicosResponse = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .getForEntity("/medicos", Medico[].class);
        assertThat(medicosResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(medicosResponse.getBody())).extracting(Medico::getId).contains(medicoId);

        ResponseEntity<BloqueoTemporal> bloqueoResponse = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/franjas/{franjaId}/bloqueos", null, BloqueoTemporal.class, franjaId);
        assertThat(bloqueoResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ConfirmarCitaRequest request = new ConfirmarCitaRequest(franjaId);
        ResponseEntity<Cita> citaResponse = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/citas", request, Cita.class);

        assertThat(citaResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(citaResponse.getBody()).isNotNull();
        assertThat(citaResponse.getBody().getFranjaHorariaId()).isEqualTo(franjaId);
        assertThat(citaResponse.getBody().getEstadoNotificacion()).isIn(
                Cita.EstadoNotificacionEnum.ENVIADA, Cita.EstadoNotificacionEnum.FALLIDA);

        // GET /citas: la cita recién confirmada MUST aparecer entre las citas activas del paciente.
        ResponseEntity<Cita[]> misCitas = como(PACIENTE_A, PACIENTE_A_PASSWORD).getForEntity("/citas", Cita[].class);
        assertThat(misCitas.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(misCitas.getBody())).extracting(Cita::getId).contains(citaResponse.getBody().getId());
    }
}
