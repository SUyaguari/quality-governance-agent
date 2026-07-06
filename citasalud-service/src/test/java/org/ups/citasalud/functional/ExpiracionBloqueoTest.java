package org.ups.citasalud.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.ups.citasalud.application.usecase.BloquearFranjaUseCase;
import org.ups.citasalud.domain.port.RelojPort;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;
import org.ups.citasalud.infrastructure.web.generated.model.ConfirmarCitaRequest;
import org.ups.citasalud.infrastructure.web.generated.model.ErrorResponse;
import org.ups.citasalud.infrastructure.web.generated.model.FranjaHoraria;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BDD (Given-When-Then), funcional. Acceptance Scenario 5 de spec.md, FR-010.
 * Usa un {@link RelojControlable} en vez de esperar 5 minutos reales.
 */
class ExpiracionBloqueoTest extends FunctionalTestBase {

    @Autowired
    private RelojPort relojPort;

    @Test
    @DisplayName("Given un bloqueo temporal, When pasan más de 5 minutos sin confirmar, Then la franja se libera automáticamente")
    void given_bloqueoTemporal_when_pasanMasDe5Minutos_then_seLiberaAutomaticamente() {
        UUID medicoId = crearMedico();
        UUID franjaId = crearFranjaDisponible(medicoId, Instant.now().plus(24, ChronoUnit.HOURS));
        RelojControlable relojControlable = (RelojControlable) relojPort;

        ResponseEntity<BloqueoTemporal> bloqueo = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/franjas/{franjaId}/bloqueos", null, BloqueoTemporal.class, franjaId);
        assertThat(bloqueo.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        relojControlable.avanzar(BloquearFranjaUseCase.DURACION_BLOQUEO.plus(Duration.ofSeconds(1)));

        // La confirmación ya no debe ser posible (FR-010/FR-013).
        ResponseEntity<ErrorResponse> intentoConfirmar = como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/citas", new ConfirmarCitaRequest(franjaId), ErrorResponse.class);
        assertThat(intentoConfirmar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(intentoConfirmar.getBody().getCode()).isEqualTo(ErrorResponse.CodeEnum.FRANJA_NO_DISPONIBLE);

        // La franja debe volver a aparecer como disponible para cualquier paciente.
        ResponseEntity<FranjaHoraria[]> franjas = como(PACIENTE_B, PACIENTE_B_PASSWORD)
                .getForEntity("/medicos/{medicoId}/franjas", FranjaHoraria[].class, medicoId);
        assertThat(List.of(franjas.getBody())).extracting(FranjaHoraria::getId).contains(franjaId);
    }

    @TestConfiguration
    static class RelojDePruebaConfig {
        @Bean
        @Primary
        RelojPort relojPort() {
            return new RelojControlable();
        }
    }
}
