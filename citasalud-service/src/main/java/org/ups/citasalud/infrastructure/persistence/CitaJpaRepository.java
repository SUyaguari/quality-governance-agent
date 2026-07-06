package org.ups.citasalud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ups.citasalud.infrastructure.persistence.model.CitaJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface CitaJpaRepository extends JpaRepository<CitaJpaEntity, UUID> {

    long countByPacienteIdAndFechaHoraCitaAfter(UUID pacienteId, Instant ahora);

    List<CitaJpaEntity> findByPacienteIdAndFechaHoraCitaAfter(UUID pacienteId, Instant ahora);
}
