package org.ups.citasalud.domain.model;

import org.ups.citasalud.domain.exception.FranjaNoDisponibleException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de dominio puro (sin anotaciones de framework, Principio I de la constitution).
 * Encapsula el ciclo de vida DISPONIBLE -> BLOQUEADA_TEMPORALMENTE -> OCUPADA descrito en
 * data-model.md, incluyendo la liberación perezosa de bloqueos vencidos (research.md §6).
 */
public class FranjaHoraria {

    private final UUID id;
    private final UUID medicoId;
    private final Instant fechaHora;
    private EstadoFranja estado;
    private UUID bloqueadaPorPacienteId;
    private Instant bloqueadaHasta;

    public FranjaHoraria(UUID id, UUID medicoId, Instant fechaHora, EstadoFranja estado,
                          UUID bloqueadaPorPacienteId, Instant bloqueadaHasta) {
        this.id = Objects.requireNonNull(id);
        this.medicoId = Objects.requireNonNull(medicoId);
        this.fechaHora = Objects.requireNonNull(fechaHora);
        this.estado = Objects.requireNonNull(estado);
        this.bloqueadaPorPacienteId = bloqueadaPorPacienteId;
        this.bloqueadaHasta = bloqueadaHasta;
    }

    public static FranjaHoraria disponible(UUID id, UUID medicoId, Instant fechaHora) {
        return new FranjaHoraria(id, medicoId, fechaHora, EstadoFranja.DISPONIBLE, null, null);
    }

    /** FR-002: solo las franjas futuras MUST ofrecerse como disponibles. */
    public boolean esFutura(Instant ahora) {
        return fechaHora.isAfter(ahora);
    }

    /** research.md §6: liberación perezosa de un bloqueo vencido (FR-010). */
    public void liberarSiBloqueoVencido(Instant ahora) {
        if (estado == EstadoFranja.BLOQUEADA_TEMPORALMENTE
                && bloqueadaHasta != null
                && !bloqueadaHasta.isAfter(ahora)) {
            estado = EstadoFranja.DISPONIBLE;
            bloqueadaPorPacienteId = null;
            bloqueadaHasta = null;
        }
    }

    public boolean estaDisponible(Instant ahora) {
        liberarSiBloqueoVencido(ahora);
        return estado == EstadoFranja.DISPONIBLE;
    }

    /** FR-009: bloquea la franja a favor de un paciente por la duración indicada. */
    public void bloquearPara(UUID pacienteId, Instant ahora, Duration duracionBloqueo) {
        if (!estaDisponible(ahora)) {
            throw new FranjaNoDisponibleException("La franja " + id + " no está disponible");
        }
        this.estado = EstadoFranja.BLOQUEADA_TEMPORALMENTE;
        this.bloqueadaPorPacienteId = Objects.requireNonNull(pacienteId);
        this.bloqueadaHasta = ahora.plus(duracionBloqueo);
    }

    /** FR-008/FR-013: confirma solo si sigue bloqueada a favor del mismo paciente y no venció. */
    public void confirmarPara(UUID pacienteId, Instant ahora) {
        liberarSiBloqueoVencido(ahora);
        boolean bloqueadaAFavorDelPaciente = estado == EstadoFranja.BLOQUEADA_TEMPORALMENTE
                && pacienteId.equals(bloqueadaPorPacienteId)
                && bloqueadaHasta != null
                && bloqueadaHasta.isAfter(ahora);
        if (!bloqueadaAFavorDelPaciente) {
            throw new FranjaNoDisponibleException("La franja " + id + " ya no está disponible para confirmar");
        }
        this.estado = EstadoFranja.OCUPADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMedicoId() {
        return medicoId;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public EstadoFranja getEstado() {
        return estado;
    }

    public UUID getBloqueadaPorPacienteId() {
        return bloqueadaPorPacienteId;
    }

    public Instant getBloqueadaHasta() {
        return bloqueadaHasta;
    }
}
