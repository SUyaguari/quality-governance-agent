package org.ups.citasalud.infrastructure.persistence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.model.EstadoNotificacion;

import java.time.Instant;
import java.util.UUID;

/** Entidad JPA — distinta del modelo de dominio {@link Cita} (Principio I). */
@Entity
@Table(name = "cita")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CitaJpaEntity {

    @Id
    private UUID id;

    private UUID pacienteId;

    private UUID medicoId;

    private UUID franjaHorariaId;

    private Instant fechaHoraCita;

    @Enumerated(EnumType.STRING)
    private EstadoNotificacion estadoNotificacion;

    private int intentosNotificacion;

    public static CitaJpaEntity fromDomain(Cita cita) {
        return new CitaJpaEntity(
                cita.getId(),
                cita.getPacienteId(),
                cita.getMedicoId(),
                cita.getFranjaHorariaId(),
                cita.getFechaHoraCita(),
                cita.getEstadoNotificacion(),
                cita.getIntentosNotificacion());
    }

    public Cita toDomain() {
        return new Cita(id, pacienteId, medicoId, franjaHorariaId, fechaHoraCita, estadoNotificacion, intentosNotificacion);
    }
}
