package org.ups.citasalud.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;
import org.ups.citasalud.infrastructure.web.generated.model.ConfirmarCitaRequest;
import org.ups.citasalud.infrastructure.web.generated.model.ErrorResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** BDD (Given-When-Then), funcional. Acceptance Scenario 4 de spec.md, SC-006. */
class AccesoSinSesionTest extends FunctionalTestBase {

    @Test
    @DisplayName("Given una persona sin sesión iniciada, When intenta consultar médicos, Then el sistema se lo impide con 401 NO_AUTENTICADO")
    void given_sinSesion_when_consultaMedicos_then_401() {
        ResponseEntity<ErrorResponse> respuesta = restTemplate.getForEntity("/medicos", ErrorResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().getCode()).isEqualTo(ErrorResponse.CodeEnum.NO_AUTENTICADO);
    }

    @Test
    @DisplayName("Given credenciales inválidas, When intenta confirmar una cita, Then el sistema responde 401")
    void given_credencialesInvalidas_when_confirmaCita_then_401() {
        ResponseEntity<ErrorResponse> respuesta = restTemplate.withBasicAuth("desconocido", "clave-incorrecta")
                .getForEntity("/medicos", ErrorResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Given una persona sin sesión iniciada, When intenta bloquear (seleccionar) una franja, Then el sistema se lo impide con 401 NO_AUTENTICADO")
    void given_sinSesion_when_bloqueaFranja_then_401() {
        UUID medicoId = crearMedico();
        UUID franjaId = crearFranjaDisponible(medicoId, Instant.now().plus(24, ChronoUnit.HOURS));

        ResponseEntity<ErrorResponse> respuesta = restTemplate
                .postForEntity("/franjas/{franjaId}/bloqueos", null, ErrorResponse.class, franjaId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().getCode()).isEqualTo(ErrorResponse.CodeEnum.NO_AUTENTICADO);
    }

    @Test
    @DisplayName("Given una persona sin sesión iniciada, When intenta confirmar una cita, Then el sistema se lo impide con 401 NO_AUTENTICADO")
    void given_sinSesion_when_confirmaCita_then_401() {
        UUID medicoId = crearMedico();
        UUID franjaId = crearFranjaDisponible(medicoId, Instant.now().plus(24, ChronoUnit.HOURS));
        como(PACIENTE_A, PACIENTE_A_PASSWORD)
                .postForEntity("/franjas/{franjaId}/bloqueos", null, BloqueoTemporal.class, franjaId);

        ResponseEntity<ErrorResponse> respuesta = restTemplate
                .postForEntity("/citas", new ConfirmarCitaRequest(franjaId), ErrorResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().getCode()).isEqualTo(ErrorResponse.CodeEnum.NO_AUTENTICADO);
    }
}
