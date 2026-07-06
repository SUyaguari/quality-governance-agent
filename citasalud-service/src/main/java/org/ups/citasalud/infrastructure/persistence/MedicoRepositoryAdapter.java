package org.ups.citasalud.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.ups.citasalud.domain.model.Medico;
import org.ups.citasalud.domain.port.MedicoRepositoryPort;

import java.util.List;
import java.util.UUID;

@Component
public class MedicoRepositoryAdapter implements MedicoRepositoryPort {

    private final MedicoJpaRepository jpaRepository;

    public MedicoRepositoryAdapter(MedicoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Medico> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(entity -> entity.toDomain())
                .toList();
    }

    @Override
    public boolean existe(UUID medicoId) {
        return jpaRepository.existsById(medicoId);
    }
}
