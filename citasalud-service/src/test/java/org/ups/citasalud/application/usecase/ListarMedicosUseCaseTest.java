package org.ups.citasalud.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ups.citasalud.domain.model.Medico;
import org.ups.citasalud.domain.port.MedicoRepositoryPort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** BDD (Given-When-Then). FR-001. */
@ExtendWith(MockitoExtension.class)
class ListarMedicosUseCaseTest {

    @Mock
    private MedicoRepositoryPort medicoRepositoryPort;

    @Test
    @DisplayName("Given médicos existentes, When se listan, Then retorna todos los médicos (FR-001)")
    void given_medicosExistentes_when_ejecutar_then_retornaTodos() {
        Medico medico = new Medico(UUID.randomUUID(), "Dra. Ana Torres");
        when(medicoRepositoryPort.listarTodos()).thenReturn(List.of(medico));

        ListarMedicosUseCase useCase = new ListarMedicosUseCase(medicoRepositoryPort);

        List<Medico> resultado = useCase.ejecutar();

        assertThat(resultado).containsExactly(medico);
    }
}
