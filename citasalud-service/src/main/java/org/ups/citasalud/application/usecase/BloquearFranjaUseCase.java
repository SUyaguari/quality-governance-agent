package org.ups.citasalud.application.usecase;

import org.ups.citasalud.domain.exception.FranjaNoDisponibleException;
import org.ups.citasalud.domain.exception.FranjaNoEncontradaException;
import org.ups.citasalud.domain.port.FranjaHorariaRepositoryPort;
import org.ups.citasalud.domain.port.RelojPort;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** FR-003/FR-004/FR-009: selecciona y bloquea temporalmente una franja disponible. */
public class BloquearFranjaUseCase {

    /** FR-010, Decisión de alcance #3: 5 minutos. */
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(5);

    private final FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    private final RelojPort relojPort;

    public BloquearFranjaUseCase(FranjaHorariaRepositoryPort franjaHorariaRepositoryPort, RelojPort relojPort) {
        this.franjaHorariaRepositoryPort = franjaHorariaRepositoryPort;
        this.relojPort = relojPort;
    }

    /** @return el instante hasta el cual queda bloqueada la franja. */
    public Instant ejecutar(UUID franjaId, UUID pacienteId) {
        franjaHorariaRepositoryPort.buscarPorId(franjaId)
                .orElseThrow(() -> new FranjaNoEncontradaException("No existe una franja con id " + franjaId));

        Instant ahora = relojPort.ahora();
        boolean bloqueada = franjaHorariaRepositoryPort.bloquearSiDisponible(franjaId, pacienteId, ahora, DURACION_BLOQUEO);
        if (!bloqueada) {
            throw new FranjaNoDisponibleException("La franja " + franjaId + " no está disponible");
        }
        return ahora.plus(DURACION_BLOQUEO);
    }
}
