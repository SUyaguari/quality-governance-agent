package org.ups.citasalud.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citasalud.domain.exception.MedicoNoEncontradoException;
import org.ups.citasalud.domain.model.EstadoFranja;
import org.ups.citasalud.domain.model.FranjaHoraria;
import org.ups.citasalud.domain.port.FranjaHorariaRepositoryPort;
import org.ups.citasalud.domain.port.MedicoRepositoryPort;
import org.ups.citasalud.domain.port.RelojPort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** BDD (Given-When-Then), Ports doblados. FR-001/FR-002. */
@ExtendWith(MockitoExtension.class)
class ConsultarFranjasDisponiblesUseCaseTest {

    @Mock
    private FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    @Mock
    private MedicoRepositoryPort medicoRepositoryPort;
    @Mock
    private RelojPort relojPort;

    @Test
    @DisplayName("Given un médico existente con franjas disponibles, When se consulta, Then retorna esas franjas (FR-001/FR-002)")
    void given_medicoExistente_when_ejecutar_then_retornaFranjasDisponibles() {
        UUID medicoId = UUID.randomUUID();
        Instant ahora = Instant.now();
        FranjaHoraria franja = FranjaHoraria.disponible(UUID.randomUUID(), medicoId, ahora.plusSeconds(3600));

        when(relojPort.ahora()).thenReturn(ahora);
        when(medicoRepositoryPort.existe(medicoId)).thenReturn(true);
        when(franjaHorariaRepositoryPort.listarDisponibles(medicoId, ahora)).thenReturn(List.of(franja));

        ConsultarFranjasDisponiblesUseCase useCase =
                new ConsultarFranjasDisponiblesUseCase(franjaHorariaRepositoryPort, medicoRepositoryPort, relojPort);

        List<FranjaHoraria> resultado = useCase.ejecutar(medicoId);

        assertThat(resultado).containsExactly(franja);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoFranja.DISPONIBLE);
    }

    @Test
    @DisplayName("Given un médico inexistente, When se consulta, Then lanza MedicoNoEncontradoException")
    void given_medicoInexistente_when_ejecutar_then_lanzaExcepcion() {
        UUID medicoId = UUID.randomUUID();
        when(medicoRepositoryPort.existe(medicoId)).thenReturn(false);

        ConsultarFranjasDisponiblesUseCase useCase =
                new ConsultarFranjasDisponiblesUseCase(franjaHorariaRepositoryPort, medicoRepositoryPort, relojPort);

        assertThatThrownBy(() -> useCase.ejecutar(medicoId)).isInstanceOf(MedicoNoEncontradoException.class);
    }
}
