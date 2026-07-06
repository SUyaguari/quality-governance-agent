package org.ups.citasalud.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ups.citasalud.domain.model.FranjaHoraria;
import org.ups.citasalud.domain.port.FranjaHorariaRepositoryPort;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FranjaHorariaRepositoryAdapter implements FranjaHorariaRepositoryPort {

    private final FranjaHorariaJpaRepository jpaRepository;

    public FranjaHorariaRepositoryAdapter(FranjaHorariaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<FranjaHoraria> buscarPorId(UUID franjaId) {
        return jpaRepository.findById(franjaId).map(entity -> entity.toDomain());
    }

    @Override
    public List<FranjaHoraria> listarDisponibles(UUID medicoId, Instant ahora) {
        return jpaRepository.buscarDisponibles(medicoId, ahora).stream()
                .map(entity -> entity.toDomain())
                .toList();
    }

    @Override
    @Transactional
    public boolean bloquearSiDisponible(UUID franjaId, UUID pacienteId, Instant ahora, Duration duracionBloqueo) {
        int filasActualizadas = jpaRepository.bloquearSiDisponible(franjaId, pacienteId, ahora.plus(duracionBloqueo), ahora);
        return filasActualizadas > 0;
    }

    @Override
    @Transactional
    public boolean confirmarSiBloqueadaPorPaciente(UUID franjaId, UUID pacienteId, Instant ahora) {
        int filasActualizadas = jpaRepository.confirmarSiBloqueadaPorPaciente(franjaId, pacienteId, ahora);
        return filasActualizadas > 0;
    }
}
