package org.ups.citasalud.infrastructure.web;

import org.springframework.stereotype.Component;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.model.FranjaHoraria;
import org.ups.citasalud.domain.model.Medico;
import org.ups.citasalud.infrastructure.web.generated.model.BloqueoTemporal;

import java.time.Instant;
import java.time.ZoneOffset;

/** Mapea entre los modelos de dominio y los DTOs generados por openapi-generator (Principio I). */
@Component
public class WebMapper {

    public org.ups.citasalud.infrastructure.web.generated.model.Medico aDto(Medico medico) {
        return new org.ups.citasalud.infrastructure.web.generated.model.Medico(medico.getId(), medico.getNombre());
    }

    public org.ups.citasalud.infrastructure.web.generated.model.FranjaHoraria aDto(FranjaHoraria franja) {
        return new org.ups.citasalud.infrastructure.web.generated.model.FranjaHoraria(
                franja.getId(),
                franja.getMedicoId(),
                franja.getFechaHora().atOffset(ZoneOffset.UTC),
                org.ups.citasalud.infrastructure.web.generated.model.FranjaHoraria.EstadoEnum.valueOf(franja.getEstado().name()));
    }

    public org.ups.citasalud.infrastructure.web.generated.model.Cita aDto(Cita cita) {
        return new org.ups.citasalud.infrastructure.web.generated.model.Cita(
                cita.getId(),
                cita.getMedicoId(),
                cita.getFranjaHorariaId(),
                cita.getFechaHoraCita().atOffset(ZoneOffset.UTC),
                org.ups.citasalud.infrastructure.web.generated.model.Cita.EstadoNotificacionEnum.valueOf(cita.getEstadoNotificacion().name()));
    }

    public BloqueoTemporal aBloqueoTemporalDto(java.util.UUID franjaId, Instant bloqueadaHasta) {
        return new BloqueoTemporal(franjaId, bloqueadaHasta.atOffset(ZoneOffset.UTC));
    }
}
