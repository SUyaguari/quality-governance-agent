package org.ups.citasalud.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.model.PacienteAutenticado;
import org.ups.citasalud.domain.port.CitaRepositoryPort;
import org.ups.citasalud.domain.port.PacienteAutenticadoPort;
import org.ups.citasalud.domain.port.RelojPort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** BDD (Given-When-Then). Solo lista citas del paciente autenticado (FR-018, GET /citas). */
@ExtendWith(MockitoExtension.class)
class ListarMisCitasActivasUseCaseTest {

    @Mock
    private CitaRepositoryPort citaRepositoryPort;
    @Mock
    private PacienteAutenticadoPort pacienteAutenticadoPort;
    @Mock
    private RelojPort relojPort;

    @Test
    @DisplayName("Given citas activas del paciente autenticado, When se listan, Then retorna solo las de ese paciente")
    void given_citasDelPaciente_when_ejecutar_then_retornaSoloLasSuyas() {
        Instant ahora = Instant.parse("2026-08-01T10:00:00Z");
        PacienteAutenticado paciente = new PacienteAutenticado(UUID.randomUUID(), "María Pérez", "0102030405", "+593987654321");
        Cita cita = Cita.nueva(UUID.randomUUID(), paciente.id(), UUID.randomUUID(), UUID.randomUUID(), ahora.plusSeconds(3600));

        when(pacienteAutenticadoPort.obtenerActual()).thenReturn(paciente);
        when(relojPort.ahora()).thenReturn(ahora);
        when(citaRepositoryPort.listarActivasPorPaciente(paciente.id(), ahora)).thenReturn(List.of(cita));

        ListarMisCitasActivasUseCase useCase =
                new ListarMisCitasActivasUseCase(citaRepositoryPort, pacienteAutenticadoPort, relojPort);

        List<Cita> resultado = useCase.ejecutar();

        assertThat(resultado).containsExactly(cita);
        assertThat(resultado).allMatch(c -> c.getPacienteId().equals(paciente.id()));
    }
}
