package org.ups.citasalud.domain.port;

import org.ups.citasalud.domain.model.Cita;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CitaRepositoryPort {

    /** FR-011: registra o actualiza una cita (p. ej. tras un cambio de estadoNotificacion, FR-016). */
    Cita guardar(Cita cita);

    /** FR-018: cuenta las citas activas (franja futura) de un paciente. */
    int contarActivasPorPaciente(UUID pacienteId, Instant ahora);

    /** Usado por ListarMisCitasActivasUseCase (GET /citas). */
    List<Cita> listarActivasPorPaciente(UUID pacienteId, Instant ahora);
}
