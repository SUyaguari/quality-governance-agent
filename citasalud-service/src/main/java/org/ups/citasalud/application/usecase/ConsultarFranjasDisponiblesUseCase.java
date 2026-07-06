package org.ups.citasalud.application.usecase;

import org.ups.citasalud.domain.exception.MedicoNoEncontradoException;
import org.ups.citasalud.domain.model.FranjaHoraria;
import org.ups.citasalud.domain.port.FranjaHorariaRepositoryPort;
import org.ups.citasalud.domain.port.MedicoRepositoryPort;
import org.ups.citasalud.domain.port.RelojPort;

import java.util.List;
import java.util.UUID;

/** FR-001/FR-002: consulta de franjas disponibles de un médico. */
public class ConsultarFranjasDisponiblesUseCase {

    private final FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    private final MedicoRepositoryPort medicoRepositoryPort;
    private final RelojPort relojPort;

    public ConsultarFranjasDisponiblesUseCase(FranjaHorariaRepositoryPort franjaHorariaRepositoryPort,
                                               MedicoRepositoryPort medicoRepositoryPort,
                                               RelojPort relojPort) {
        this.franjaHorariaRepositoryPort = franjaHorariaRepositoryPort;
        this.medicoRepositoryPort = medicoRepositoryPort;
        this.relojPort = relojPort;
    }

    public List<FranjaHoraria> ejecutar(UUID medicoId) {
        if (!medicoRepositoryPort.existe(medicoId)) {
            throw new MedicoNoEncontradoException("No existe un médico con id " + medicoId);
        }
        return franjaHorariaRepositoryPort.listarDisponibles(medicoId, relojPort.ahora());
    }
}
