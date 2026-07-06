package org.ups.citasalud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.ups.citasalud.infrastructure.persistence.model.FranjaHorariaJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface FranjaHorariaJpaRepository extends JpaRepository<FranjaHorariaJpaEntity, UUID> {

    @Query("""
            SELECT f FROM FranjaHorariaJpaEntity f
            WHERE f.medicoId = :medicoId AND f.fechaHora > :ahora
              AND (f.estado = org.ups.citasalud.domain.model.EstadoFranja.DISPONIBLE
                   OR (f.estado = org.ups.citasalud.domain.model.EstadoFranja.BLOQUEADA_TEMPORALMENTE
                       AND f.bloqueadaHasta <= :ahora))
            """)
    List<FranjaHorariaJpaEntity> buscarDisponibles(@Param("medicoId") UUID medicoId, @Param("ahora") Instant ahora);

    /** research.md §5: escritura condicional atómica (FR-009). */
    @Modifying
    @Query("""
            UPDATE FranjaHorariaJpaEntity f
            SET f.estado = org.ups.citasalud.domain.model.EstadoFranja.BLOQUEADA_TEMPORALMENTE,
                f.bloqueadaPorPacienteId = :pacienteId,
                f.bloqueadaHasta = :bloqueadaHasta
            WHERE f.id = :id
              AND (f.estado = org.ups.citasalud.domain.model.EstadoFranja.DISPONIBLE
                   OR (f.estado = org.ups.citasalud.domain.model.EstadoFranja.BLOQUEADA_TEMPORALMENTE
                       AND f.bloqueadaHasta <= :ahora))
            """)
    int bloquearSiDisponible(@Param("id") UUID id, @Param("pacienteId") UUID pacienteId,
                              @Param("bloqueadaHasta") Instant bloqueadaHasta, @Param("ahora") Instant ahora);

    /** research.md §5: escritura condicional atómica (FR-008/FR-011/FR-013). */
    @Modifying
    @Query("""
            UPDATE FranjaHorariaJpaEntity f
            SET f.estado = org.ups.citasalud.domain.model.EstadoFranja.OCUPADA
            WHERE f.id = :id
              AND f.estado = org.ups.citasalud.domain.model.EstadoFranja.BLOQUEADA_TEMPORALMENTE
              AND f.bloqueadaPorPacienteId = :pacienteId
              AND f.bloqueadaHasta > :ahora
            """)
    int confirmarSiBloqueadaPorPaciente(@Param("id") UUID id, @Param("pacienteId") UUID pacienteId,
                                         @Param("ahora") Instant ahora);
}
