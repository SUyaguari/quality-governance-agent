package org.ups.citasalud.domain.port;

import org.ups.citasalud.domain.model.FranjaHoraria;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FranjaHorariaRepositoryPort {

    Optional<FranjaHoraria> buscarPorId(UUID franjaId);

    /** FR-001/FR-002: solo franjas futuras y disponibles (bloqueos vencidos se tratan como libres). */
    List<FranjaHoraria> listarDisponibles(UUID medicoId, Instant ahora);

    /**
     * FR-009: escritura condicional atómica (research.md §5). Devuelve {@code true} si la franja
     * quedó bloqueada a favor del paciente, {@code false} si ya no estaba disponible.
     */
    boolean bloquearSiDisponible(UUID franjaId, UUID pacienteId, Instant ahora, Duration duracionBloqueo);

    /**
     * FR-008/FR-011/FR-013: escritura condicional atómica. Devuelve {@code true} si la franja
     * quedó confirmada (OCUPADA) para el paciente, {@code false} si el bloqueo ya no era válido.
     */
    boolean confirmarSiBloqueadaPorPaciente(UUID franjaId, UUID pacienteId, Instant ahora);
}
