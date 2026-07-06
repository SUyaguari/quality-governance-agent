package org.ups.citasalud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ups.citasalud.infrastructure.persistence.model.MedicoJpaEntity;

import java.util.UUID;

interface MedicoJpaRepository extends JpaRepository<MedicoJpaEntity, UUID> {
}
