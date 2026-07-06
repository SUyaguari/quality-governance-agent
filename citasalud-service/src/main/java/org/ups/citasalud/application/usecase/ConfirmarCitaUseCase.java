package org.ups.citasalud.application.usecase;

import org.ups.citasalud.domain.exception.FranjaNoDisponibleException;
import org.ups.citasalud.domain.exception.FranjaNoEncontradaException;
import org.ups.citasalud.domain.exception.LimiteCitasActivasExcedidoException;
import org.ups.citasalud.domain.model.Cita;
import org.ups.citasalud.domain.model.FranjaHoraria;
import org.ups.citasalud.domain.model.PacienteAutenticado;
import org.ups.citasalud.domain.port.CitaRepositoryPort;
import org.ups.citasalud.domain.port.FranjaHorariaRepositoryPort;
import org.ups.citasalud.domain.port.NotificacionCitaPort;
import org.ups.citasalud.domain.port.PacienteAutenticadoPort;
import org.ups.citasalud.domain.port.RelojPort;

import java.time.Instant;
import java.util.UUID;

/**
 * FR-006/FR-008/FR-011/FR-012/FR-013/FR-017/FR-018: confirma la reserva de una franja
 * previamente bloqueada por el paciente autenticado.
 */
public class ConfirmarCitaUseCase {

    /** FR-018, Decisión de alcance #4: máximo 2 citas activas por paciente. */
    public static final int LIMITE_CITAS_ACTIVAS = 2;

    private final FranjaHorariaRepositoryPort franjaHorariaRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteAutenticadoPort pacienteAutenticadoPort;
    private final NotificacionCitaPort notificacionCitaPort;
    private final RelojPort relojPort;

    public ConfirmarCitaUseCase(FranjaHorariaRepositoryPort franjaHorariaRepositoryPort,
                                 CitaRepositoryPort citaRepositoryPort,
                                 PacienteAutenticadoPort pacienteAutenticadoPort,
                                 NotificacionCitaPort notificacionCitaPort,
                                 RelojPort relojPort) {
        this.franjaHorariaRepositoryPort = franjaHorariaRepositoryPort;
        this.citaRepositoryPort = citaRepositoryPort;
        this.pacienteAutenticadoPort = pacienteAutenticadoPort;
        this.notificacionCitaPort = notificacionCitaPort;
        this.relojPort = relojPort;
    }

    public Cita ejecutar(UUID franjaId) {
        PacienteAutenticado paciente = pacienteAutenticadoPort.obtenerActual();
        Instant ahora = relojPort.ahora();

        FranjaHoraria franja = franjaHorariaRepositoryPort.buscarPorId(franjaId)
                .orElseThrow(() -> new FranjaNoEncontradaException("No existe una franja con id " + franjaId));

        // FR-018: se valida antes de intentar confirmar (ver ConfirmarCitaUseCase, nota de diseño
        // en research.md — el conteo no usa lock distribuido, consistente con YAGNI).
        int citasActivas = citaRepositoryPort.contarActivasPorPaciente(paciente.id(), ahora);
        if (citasActivas >= LIMITE_CITAS_ACTIVAS) {
            throw new LimiteCitasActivasExcedidoException(
                    "El paciente " + paciente.id() + " ya tiene " + citasActivas + " citas activas");
        }

        // FR-008/FR-012/FR-013: escritura condicional atómica — también protege contra una
        // solicitud de confirmación duplicada (doble clic/reintento), ya que la segunda llamada
        // encontrará la franja ya OCUPADA y fallará aquí.
        boolean confirmada = franjaHorariaRepositoryPort.confirmarSiBloqueadaPorPaciente(franjaId, paciente.id(), ahora);
        if (!confirmada) {
            throw new FranjaNoDisponibleException("La franja " + franjaId + " ya no está disponible para confirmar");
        }

        Cita cita = citaRepositoryPort.guardar(
                Cita.nueva(UUID.randomUUID(), paciente.id(), franja.getMedicoId(), franjaId, franja.getFechaHora()));

        // FR-014/FR-016/FR-017: el envío (con reintentos) nunca revierte el registro ya hecho.
        try {
            notificacionCitaPort.enviarConfirmacion(cita, paciente.nombreCompleto(), paciente.numeroWhatsApp());
            cita.marcarNotificacionEnviada();
        } catch (NotificacionCitaPort.NotificacionFallidaException e) {
            cita.registrarIntentoFallido();
        }
        return citaRepositoryPort.guardar(cita);
    }
}
