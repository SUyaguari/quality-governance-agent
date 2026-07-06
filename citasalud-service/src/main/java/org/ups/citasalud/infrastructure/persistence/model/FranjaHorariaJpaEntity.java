package org.ups.citasalud.infrastructure.persistence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ups.citasalud.domain.model.EstadoFranja;
import org.ups.citasalud.domain.model.FranjaHoraria;

import java.time.Instant;
import java.util.UUID;

/** Entidad JPA — distinta del modelo de dominio {@link FranjaHoraria} (Principio I). */
@Entity
@Table(name = "franja_horaria", uniqueConstraints = @UniqueConstraint(columnNames = {"medico_id", "fecha_hora"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FranjaHorariaJpaEntity {

    @Id
    private UUID id;

    private UUID medicoId;

    private Instant fechaHora;

    @Enumerated(EnumType.STRING)
    private EstadoFranja estado;

    private UUID bloqueadaPorPacienteId;

    private Instant bloqueadaHasta;

    public FranjaHoraria toDomain() {
        return new FranjaHoraria(id, medicoId, fechaHora, estado, bloqueadaPorPacienteId, bloqueadaHasta);
    }
}
