package org.ups.citasalud.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citasalud.domain.exception.FranjaNoDisponibleException;
import org.ups.citasalud.domain.exception.FranjaNoEncontradaException;
import org.ups.citasalud.domain.model.FranjaHoraria;
import org.ups.citasalud.domain.port.FranjaHorariaRepositoryPort;
import org.ups.citasalud.domain.port.RelojPort;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** BDD (Given-When-Then), Ports doblados. FR-003/FR-004/FR-009. */
@ExtendWith(MockitoExtension.class)
class BloquearFranjaUseCaseTest {

    @Mock
    private FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    @Mock
    private RelojPort relojPort;

    @Test
    @DisplayName("Given una franja existente y disponible, When el paciente la selecciona, Then queda bloqueada por 5 minutos (FR-003/FR-009)")
    void given_franjaDisponible_when_ejecutar_then_quedaBloqueada() {
        UUID franjaId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        Instant ahora = Instant.parse("2026-08-01T10:00:00Z");
        FranjaHoraria franja = FranjaHoraria.disponible(franjaId, UUID.randomUUID(), ahora.plusSeconds(3600));

        when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.of(franja));
        when(relojPort.ahora()).thenReturn(ahora);
        when(franjaHorariaRepositoryPort.bloquearSiDisponible(eq(franjaId), eq(pacienteId), eq(ahora), any()))
                .thenReturn(true);

        BloquearFranjaUseCase useCase = new BloquearFranjaUseCase(franjaHorariaRepositoryPort, relojPort);

        Instant bloqueadaHasta = useCase.ejecutar(franjaId, pacienteId);

        assertThat(bloqueadaHasta).isEqualTo(ahora.plus(BloquearFranjaUseCase.DURACION_BLOQUEO));
    }

    @Test
    @DisplayName("Given una franja inexistente, When se intenta bloquear, Then lanza FranjaNoEncontradaException")
    void given_franjaInexistente_when_ejecutar_then_lanzaFranjaNoEncontrada() {
        UUID franjaId = UUID.randomUUID();
        when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.empty());

        BloquearFranjaUseCase useCase = new BloquearFranjaUseCase(franjaHorariaRepositoryPort, relojPort);

        assertThatThrownBy(() -> useCase.ejecutar(franjaId, UUID.randomUUID()))
                .isInstanceOf(FranjaNoEncontradaException.class);
    }

    @Test
    @DisplayName("Given una franja ya no disponible, When se intenta bloquear, Then lanza FranjaNoDisponibleException (FR-004)")
    void given_franjaNoDisponible_when_ejecutar_then_lanzaFranjaNoDisponible() {
        UUID franjaId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        Instant ahora = Instant.parse("2026-08-01T10:00:00Z");
        FranjaHoraria franja = FranjaHoraria.disponible(franjaId, UUID.randomUUID(), ahora.plusSeconds(3600));

        when(franjaHorariaRepositoryPort.buscarPorId(franjaId)).thenReturn(Optional.of(franja));
        when(relojPort.ahora()).thenReturn(ahora);
        when(franjaHorariaRepositoryPort.bloquearSiDisponible(eq(franjaId), eq(pacienteId), eq(ahora), any()))
                .thenReturn(false);

        BloquearFranjaUseCase useCase = new BloquearFranjaUseCase(franjaHorariaRepositoryPort, relojPort);

        assertThatThrownBy(() -> useCase.ejecutar(franjaId, pacienteId))
                .isInstanceOf(FranjaNoDisponibleException.class);
    }
}
