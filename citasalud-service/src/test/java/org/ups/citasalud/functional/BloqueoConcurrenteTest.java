package org.ups.citasalud.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;
import org.ups.citasalud.infrastructure.web.generated.model.ErrorResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

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

    @Test
    @DisplayName("Given una franja disponible, When varios hilos intentan bloquearla verdaderamente al mismo tiempo, Then la escritura condicional en BD garantiza que exactamente uno tiene éxito (FR-009)")
    void given_franjaDisponible_when_hilosConcurrentesBloqueanALaVez_then_soloUnoTieneExito() throws InterruptedException {
        UUID medicoId = crearMedico();
        UUID franjaId = crearFranjaDisponible(medicoId, Instant.now().plus(24, ChronoUnit.HOURS));

        int hilos = 8;
        CyclicBarrier salidaSincronizada = new CyclicBarrier(hilos);
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        try {
            List<Callable<HttpStatus>> intentos = IntStream.range(0, hilos)
                    .<Callable<HttpStatus>>mapToObj(i -> () -> {
                        String usuario = i % 2 == 0 ? PACIENTE_A : PACIENTE_B;
                        String password = i % 2 == 0 ? PACIENTE_A_PASSWORD : PACIENTE_B_PASSWORD;
                        salidaSincronizada.await();
                        ResponseEntity<String> respuesta = como(usuario, password)
                                .postForEntity("/franjas/{franjaId}/bloqueos", null, String.class, franjaId);
                        return HttpStatus.valueOf(respuesta.getStatusCode().value());
                    })
                    .toList();

            List<Future<HttpStatus>> resultados = executor.invokeAll(intentos);
            long exitosos = resultados.stream().filter(f -> estadoDe(f) == HttpStatus.CREATED).count();
            long rechazados = resultados.stream().filter(f -> estadoDe(f) == HttpStatus.CONFLICT).count();

            assertThat(exitosos).isEqualTo(1);
            assertThat(rechazados).isEqualTo(hilos - 1L);
        } finally {
            executor.shutdown();
        }
    }

    private static HttpStatus estadoDe(Future<HttpStatus> resultado) {
        try {
            return resultado.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
