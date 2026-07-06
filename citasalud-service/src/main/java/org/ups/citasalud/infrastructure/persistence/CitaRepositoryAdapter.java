package org.ups.citasalud.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.port.CitaRepositoryPort;
import org.ups.citasalud.infrastructure.persistence.model.CitaJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class CitaRepositoryAdapter implements CitaRepositoryPort {

    private final CitaJpaRepository jpaRepository;

    public CitaRepositoryAdapter(CitaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Cita guardar(Cita cita) {
        CitaJpaEntity guardada = jpaRepository.save(CitaJpaEntity.fromDomain(cita));
        return guardada.toDomain();
    }

    @Override
    public int contarActivasPorPaciente(UUID pacienteId, Instant ahora) {
        return (int) jpaRepository.countByPacienteIdAndFechaHoraCitaAfter(pacienteId, ahora);
    }

    @Override
    public List<Cita> listarActivasPorPaciente(UUID pacienteId, Instant ahora) {
        return jpaRepository.findByPacienteIdAndFechaHoraCitaAfter(pacienteId, ahora).stream()
                .map(entity -> entity.toDomain())
                .toList();
    }
}
