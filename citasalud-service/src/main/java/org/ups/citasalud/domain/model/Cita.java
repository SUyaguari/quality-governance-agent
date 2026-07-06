package org.ups.citasalud.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Modelo de dominio puro. FR-011/FR-014/FR-016/FR-017. */
public class Cita {

    private final UUID id;
    private final UUID pacienteId;
    private final UUID medicoId;
    private final UUID franjaHorariaId;
    private final Instant fechaHoraCita;
    private EstadoNotificacion estadoNotificacion;
    private int intentosNotificacion;

    public Cita(UUID id, UUID pacienteId, UUID medicoId, UUID franjaHorariaId, Instant fechaHoraCita,
                EstadoNotificacion estadoNotificacion, int intentosNotificacion) {
        this.id = Objects.requireNonNull(id);
        this.pacienteId = Objects.requireNonNull(pacienteId);
        this.medicoId = Objects.requireNonNull(medicoId);
        this.franjaHorariaId = Objects.requireNonNull(franjaHorariaId);
        this.fechaHoraCita = Objects.requireNonNull(fechaHoraCita);
        this.estadoNotificacion = Objects.requireNonNull(estadoNotificacion);
        this.intentosNotificacion = intentosNotificacion;
    }

    public static Cita nueva(UUID id, UUID pacienteId, UUID medicoId, UUID franjaHorariaId, Instant fechaHoraCita) {
        return new Cita(id, pacienteId, medicoId, franjaHorariaId, fechaHoraCita, EstadoNotificacion.PENDIENTE, 0);
    }

    /** FR-016/FR-017: el registro de la cita nunca se revierte por esto, solo se refleja el resultado. */
    public void marcarNotificacionEnviada() {
        this.estadoNotificacion = EstadoNotificacion.ENVIADA;
    }

    public void registrarIntentoFallido() {
        this.intentosNotificacion++;
        this.estadoNotificacion = EstadoNotificacion.FALLIDA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public UUID getMedicoId() {
        return medicoId;
    }

    public UUID getFranjaHorariaId() {
        return franjaHorariaId;
    }

    public Instant getFechaHoraCita() {
        return fechaHoraCita;
    }

    public EstadoNotificacion getEstadoNotificacion() {
        return estadoNotificacion;
    }

    public int getIntentosNotificacion() {
        return intentosNotificacion;
    }
}
