package org.ups.citasalud.application.usecase;

import org.ups.citasalud.domain.model.Medico;
import org.ups.citasalud.domain.port.MedicoRepositoryPort;

import java.util.List;

/** FR-001: lista los médicos disponibles para agendar. */
public class ListarMedicosUseCase {

    private final MedicoRepositoryPort medicoRepositoryPort;

    public ListarMedicosUseCase(MedicoRepositoryPort medicoRepositoryPort) {
        this.medicoRepositoryPort = medicoRepositoryPort;
    }

    public List<Medico> ejecutar() {
        return medicoRepositoryPort.listarTodos();
    }
}
