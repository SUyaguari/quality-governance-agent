package org.ups.citasalud.application.usecase;

import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.model.PacienteAutenticado;
import org.ups.citasalud.domain.port.CitaRepositoryPort;
import org.ups.citasalud.domain.port.PacienteAutenticadoPort;
import org.ups.citasalud.domain.port.RelojPort;

import java.util.List;

/** GET /citas: lista las citas activas del paciente autenticado (FR-018). */
public class ListarMisCitasActivasUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteAutenticadoPort pacienteAutenticadoPort;
    private final RelojPort relojPort;

    public ListarMisCitasActivasUseCase(CitaRepositoryPort citaRepositoryPort,
                                         PacienteAutenticadoPort pacienteAutenticadoPort,
                                         RelojPort relojPort) {
        this.citaRepositoryPort = citaRepositoryPort;
        this.pacienteAutenticadoPort = pacienteAutenticadoPort;
        this.relojPort = relojPort;
    }

    public List<Cita> ejecutar() {
        PacienteAutenticado paciente = pacienteAutenticadoPort.obtenerActual();
        return citaRepositoryPort.listarActivasPorPaciente(paciente.id(), relojPort.ahora());
    }
}
